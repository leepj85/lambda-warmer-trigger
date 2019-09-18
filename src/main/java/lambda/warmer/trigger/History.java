package lambda.warmer.trigger;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

import java.util.Date;


@DynamoDBDocument
public class History {

    private String date;
    private String action;

    public History(){}

    public History(String action) {
        this.date = new Date().toString();
        this.action = action;
    }
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public  String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
