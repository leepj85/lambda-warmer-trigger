/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package lambda.warmer.trigger;

import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Library {

    private DynamoDB dynamoDb;
    private String DYNAMODB_TABLE_NAME = "taskmaster";
    private Regions REGION = Regions.US_WEST_2;
    final AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.defaultClient();
    private DynamoDBMapper ddbMapper = new DynamoDBMapper(ddb);

    public APIGatewayProxyResponseEvent updateAssignee(APIGatewayProxyRequestEvent event){
        //convert path variables.
        Map<String,String> urlParameterMap = event.getPathParameters();
        String id = urlParameterMap.get("id");
        String assignee = urlParameterMap.get("assignee");

        Task t = ddbMapper.load(Task.class, id);
        t.setAssignee(assignee);
        t.setStatus("assigned");
        t.addToHistory(new History("assigned"));
        ddbMapper.save(t);

        Gson gson = new Gson();
        String json = gson.toJson(t);

        APIGatewayProxyResponseEvent res = new APIGatewayProxyResponseEvent();
        res.setBody(json);

        return res;
    }

    public APIGatewayProxyResponseEvent updateState(APIGatewayProxyRequestEvent event){
        //convert path variables.
        Map<String,String> urlParameterMap = event.getPathParameters();
        String id = urlParameterMap.get("id");

        //query db for Task using id and change state.
        Task t = ddbMapper.load(Task.class, id);
        if(t.getStatus().equals("available")){
            t.setStatus("assigned");
        } else if(t.getStatus().equals("assigned")){
            t.setStatus("accepted");
        } else if(t.getStatus().equals("accepted")){
            t.setStatus("finished");
        }
        t.addToHistory(new History(t.getStatus()));
        ddbMapper.save(t);

        Gson gson = new Gson();
        String json = gson.toJson(t);

        APIGatewayProxyResponseEvent res = new APIGatewayProxyResponseEvent();
        res.setBody(json);

        return res;
    }

    public APIGatewayProxyResponseEvent getAllTasks() {
        List<Task> tasks = ddbMapper.scan(Task.class, new DynamoDBScanExpression());

        Gson gson = new Gson();
        String json = gson.toJson(tasks);
        APIGatewayProxyResponseEvent res = new APIGatewayProxyResponseEvent();
        res.setBody(json);
        return res;
    }

    public APIGatewayProxyResponseEvent getUserTasks(APIGatewayProxyRequestEvent event){
        //convert path variables.
        Map<String,String> urlParameterMap = event.getPathParameters();

        String assignee = urlParameterMap.get("user");
        //set up to query with specific filter for all tasks where assignee = user
        HashMap<String, AttributeValue> eav = new HashMap<>();
        eav.put(":v1", new AttributeValue().withS(assignee));
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("assignee = :v1").withExpressionAttributeValues((eav));

        //Tasks returned from querying sent to db.
        List<Task> scanResult = ddbMapper.scan(Task.class, scanExpression);

        Gson gson = new Gson();
        String json = gson.toJson(scanResult);

        APIGatewayProxyResponseEvent res = new APIGatewayProxyResponseEvent();
        res.setBody(json);
        return res;
    }

    public Task createTask(Task task){
        Task t = new Task(task.getTitle(), task.getDescription(), task.getAssignee());
        if(task.getAssignee() == null){
            History history = new History("Task was created and is not assigned");
            t.addToHistory(history);
        } else {
            t.setAssignee(task.getAssignee());
            t.setStatus("assigned");
            History history = new History("Task was created and assigned to " + task.getAssignee());
            t.addToHistory(history);
        }
        ddbMapper.save(t);
        return task;
    }

    public Task deleteTask(Task task) {
        Task t = ddbMapper.load(Task.class, task.getId());
        ddbMapper.delete(t);
        return t;
    }

}
