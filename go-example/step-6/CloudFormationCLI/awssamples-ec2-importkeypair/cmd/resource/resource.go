package resource

import (
	"fmt"
	"log"

	"github.com/aws-cloudformation/cloudformation-cli-go-plugin/cfn/handler"
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/awserr"
	"github.com/aws/aws-sdk-go/service/cloudformation"
	"github.com/aws/aws-sdk-go/service/ec2"
)

// Create handles the Create event from the Cloudformation service.
func Create(req handler.Request, prevModel *Model, currentModel *Model) (handler.ProgressEvent, error) {
	pairName := currentModel.KeyName
	// First we check to see if we are stabilizing.
	// We do this by checking the callback CallbackContext
	v, ok := req.CallbackContext["Stabilizing"].(bool)
	if ok {
		if v {
			// We use the readhandler to check if the resource exist.
			// If it does not, we return an inProgress event and continue stabilizing.
			// If it exist, we return Success.
			r, _ := Read(req, prevModel, currentModel)
			switch r.OperationStatus {
			case handler.Failed:
				return handler.ProgressEvent{
					OperationStatus:      handler.InProgress,
					CallbackDelaySeconds: 5,
					CallbackContext:      req.CallbackContext,
					Message:              "In progress",
					ResourceModel:        currentModel,
				}, nil
			case handler.Success:
				return handler.ProgressEvent{
					OperationStatus: handler.Success,
					Message:         "Success",
					ResourceModel:   currentModel,
				}, nil
			}
		}
	}
	// We create a an EC2 service client using the supplied session.
	svc := ec2.New(req.Session)

	// Next, we make the call to import the keypair and handle any errors
	// by logging and returning a failed progress event.
	in := &ec2.ImportKeyPairInput{
		KeyName:           pairName,
		DryRun:            aws.Bool(false),
		PublicKeyMaterial: []byte(*currentModel.PublicKeyMaterial),
		TagSpecifications: convertToTagSpec(currentModel.Tags),
	}
	r, err := svc.ImportKeyPair(in)
	if err != nil {
		if aerr, ok := err.(awserr.Error); ok && aerr.Code() == "InvalidKeyPair.Duplicate" {
			return reportError(fmt.Sprintf("Keypair %s already exists.", *pairName), cloudformation.ErrCodeAlreadyExistsException), nil
		}
		return reportError(fmt.Sprintf("Unable to create key pair: %s, %s.", *pairName, err.Error()), cloudformation.HandlerErrorCodeInternalFailure), nil
	}
	currentModel.KeyPairId = r.KeyPairId
	currentModel.KeyFingerprint = r.KeyFingerprint

	// Finally, we return an inProgress event.
	c := map[string]interface{}{"Stabilizing": true}
	return handler.ProgressEvent{
		OperationStatus:      handler.InProgress,
		CallbackDelaySeconds: 5,
		CallbackContext:      c,
		Message:              "In progress",
		ResourceModel:        currentModel,
	}, nil
}

// Read handles the Read event from the Cloudformation service.
func Read(req handler.Request, prevModel *Model, currentModel *Model) (handler.ProgressEvent, error) {
	// We create a an EC2 service client using the supplied session.
	svc := ec2.New(req.Session)

	// Next, we make the call to import the key pair and handle any errors
	// by logging and returning a failed progress event.
	result, err := svc.DescribeKeyPairs(&ec2.DescribeKeyPairsInput{
		DryRun:     aws.Bool(false),
		KeyPairIds: []*string{currentModel.KeyPairId},
	})
	if err != nil {
		return reportError(fmt.Sprintf("Unable to read key pair: %s, %s.", *currentModel.KeyPairId, err.Error()), cloudformation.HandlerErrorCodeNotFound), nil
	}
	currentModel.KeyFingerprint = result.KeyPairs[0].KeyFingerprint

	// Finally, we return a successful progress event.
	return handler.ProgressEvent{
		OperationStatus: handler.Success,
		Message:         "Success",
		ResourceModel:   currentModel,
	}, nil
}

// Update handles the Update event from the Cloudformation service.
func Update(req handler.Request, prevModel *Model, currentModel *Model) (handler.ProgressEvent, error) {
	var err error

	// We create a an EC2 service client using the supplied session.
	svc := ec2.New(req.Session)

	// If the key pair is not found, return an error because we can't update
	// a key that does not exist.
	// We use the readhandler for this.
	r, _ := Read(req, prevModel, currentModel)
	if r.OperationStatus == handler.Failed {
		return reportError(fmt.Sprintf("Unable to update key pair: %s Does not exist", *currentModel.KeyPairId), cloudformation.HandlerErrorCodeNotFound), nil
	}
	// KeyPairName and KeyPairPublicKey are set as
	// createOnlyProperties in the model; specifying values for
	// such will then result in a new resource being created.  This
	// update handler is only used to update Tags as specified in
	// the template.

	// Delete existing tags
	_, err = svc.DeleteTags(&ec2.DeleteTagsInput{
		DryRun: aws.Bool(false),
		Resources: []*string{
			currentModel.KeyPairId,
		}})
	if err != nil {
		return reportError(fmt.Sprintf("Unable to update key pair: %s, %s.", *currentModel.KeyPairId, err.Error()), cloudformation.HandlerErrorCodeInternalFailure), nil
	}
	t := convertToTags(currentModel.Tags)
	_, err = svc.CreateTags(&ec2.CreateTagsInput{
		DryRun:    aws.Bool(false),
		Resources: []*string{currentModel.KeyPairId},
		Tags:      t.([]*ec2.Tag),
	})
	if err != nil {
		return reportError(fmt.Sprintf("Unable to update key pair: %s, %s.", *currentModel.KeyPairId, err.Error()), cloudformation.HandlerErrorCodeInternalFailure), nil
	}

	// Finally, we return a successful progress event.
	return handler.ProgressEvent{
		OperationStatus: handler.Success,
		Message:         "Success",
		ResourceModel:   currentModel,
	}, nil
}

// Delete handles the Delete event from the Cloudformation service.
func Delete(req handler.Request, prevModel *Model, currentModel *Model) (handler.ProgressEvent, error) {
	// We create a an EC2 service client using the supplied session.
	svc := ec2.New(req.Session)

	// If the key pair is found, return an error because we can't delete
	// a key that does not exist.
	// We use the readhandler for this.
	r, _ := Read(req, prevModel, currentModel)
	if r.OperationStatus == handler.Failed {
		return reportError(fmt.Sprintf("Unable to update key pair: %s Does not exist", *currentModel.KeyPairId), cloudformation.HandlerErrorCodeNotFound), nil
	}

	// We delete the keypair
	_, err := svc.DeleteKeyPair(&ec2.DeleteKeyPairInput{
		DryRun:    aws.Bool(false),
		KeyPairId: currentModel.KeyPairId,
		KeyName:   currentModel.KeyName,
	})

	//We handle any errors
	if err != nil {
		return reportError(err.Error(), cloudformation.HandlerErrorCodeNotUpdatable), nil
	}

	// Finally, we return a successful progress event.
	return handler.ProgressEvent{
		OperationStatus: handler.Success,
		Message:         "Success",
	}, nil
}

// List handles the List event from the Cloudformation service.
func List(req handler.Request, prevModel *Model, currentModel *Model) (handler.ProgressEvent, error) {
	// We create a model to return
	out := &Model{}

	// We create a list of keypairs
	var keys []interface{}

	// We create a an EC2 service client using the supplied session.
	svc := ec2.New(req.Session)

	// Next, we make the call to import the key pair and handle any errors
	// by logging and returning a failed progress event.
	result, err := svc.DescribeKeyPairs(&ec2.DescribeKeyPairsInput{
		DryRun: aws.Bool(false),
	})
	if err != nil {
		return reportError(err.Error(), cloudformation.ErrCodeOperationNotFoundException), nil
	}

	// We build a list of models to return
	for _, k := range result.KeyPairs {
		t := convertToTags(k.Tags)
		m := Model{}
		m.KeyFingerprint = k.KeyFingerprint
		m.KeyName = k.KeyName
		m.KeyPairId = k.KeyPairId
		m.Tags = t.([]Tag)
		keys = append(keys, m)
	}

	// Finally, we return a successful progress event.
	return handler.ProgressEvent{
		OperationStatus: handler.Success,
		Message:         "Success",
		ResourceModel:   out,
		ResourceModels:  keys,
	}, nil
}

func reportError(message string, code string) handler.ProgressEvent {
	log.Println(message)
	return handler.ProgressEvent{
		OperationStatus:  handler.Failed,
		HandlerErrorCode: code,
		Message:          "Failed",
	}
}

func convertToTagSpec(input []Tag) []*ec2.TagSpecification {
	t := convertToTags(input)
	ts := []*ec2.TagSpecification{&ec2.TagSpecification{ResourceType: aws.String("key-pair"), Tags: t.([]*ec2.Tag)}}
	return ts
}

func convertToTags(input interface{}) interface{} {

	switch t := input.(type) {
	case []Tag:
		ts := []*ec2.Tag{}
		for _, k := range t {
			t := &ec2.Tag{}
			t.Key = k.Key
			t.Value = k.Value
			ts = append(ts, t)
		}
		return ts
	case []*ec2.Tag:
		ts := []Tag{}
		for _, k := range t {
			t := Tag{}
			t.Key = k.Key
			t.Value = k.Value
			ts = append(ts, t)
		}
		return ts
		// This should never happen.
	default:
		return nil
	}
}
      
