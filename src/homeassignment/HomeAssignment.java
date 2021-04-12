package homeassignment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;

public class HomeAssignment {

    public static void main(String[] args) throws IOException, ParseException {

        /**
         * ****************************************GETTING
         * DATA*****************************************
         */
        String jsonString = "";
        String query = "https://www.wix.com/_serverless/hiring-task-spreadsheet-evaluator/jobs";

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(query);

        HttpResponse httpresponse = httpclient.execute(httpget);
        int statusCode = httpresponse.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            throw new RuntimeException("Failed with HTTP error code : " + statusCode);
        }
        HttpEntity httpEntity = httpresponse.getEntity();
        jsonString = EntityUtils.toString(httpEntity);

        JSONObject jsonObject = new JSONObject();
        JSONArray jobs;
        String link;
        String postURL = "";
        String resultStr = "{\"email\": \"andrejus.necvetnas@gmail.com\", \"results\": [";

        if (jsonString != "") {
            try {
                Object obj = new JSONParser().parse(jsonString);
                jsonObject = (JSONObject) obj;
            } catch (Throwable cause) {
                cause.printStackTrace();
            }
            String URL = (String) jsonObject.get("submissionUrl");
            postURL = URL;
            jobs = (JSONArray) jsonObject.get("jobs"); // 
            Iterator jobsItr = jobs.iterator();
            int counterItr = 0;
            while (jobsItr.hasNext()) {
                JSONObject singleJob = (JSONObject) jobsItr.next();
                String id = (String) singleJob.get("id");
                if (counterItr == 0) {
                    resultStr += "{\"id\": \"" + id + "\", \"data\": [";
                } else {
                    resultStr += ",{\"id\": \"" + id + "\", \"data\": [";
                }
                counterItr++;
                List<ArrayList<Value>> listOfValues = new ArrayList<ArrayList<Value>>();

                JSONArray data = (JSONArray) singleJob.get("data");

                Iterator dataItr = data.iterator();

                while (dataItr.hasNext()) {
                    JSONArray insideData = (JSONArray) dataItr.next();

                    ////////////////////////////////////////////////////////////
                    Iterator iDataItr = insideData.iterator();
                    listOfValues.add(new ArrayList<Value>());
                    while (iDataItr.hasNext()) {
                        JSONObject obj = (JSONObject) iDataItr.next();
                        if (obj.containsKey("value")) {
                            JSONObject value = (JSONObject) obj.get("value");
                            if (value.containsKey("boolean")) {
                                Boolean bool = (Boolean) value.get("boolean");
                                listOfValues.get(listOfValues.size() - 1).add(new Value("boolean", bool));
                            } else if (value.containsKey("number")) {
                                double num = ((Number) value.get("number")).doubleValue();
                                listOfValues.get(listOfValues.size() - 1).add(new Value("number", num));
                            } else if (value.containsKey("text")) {
                                String text = (String) value.get("text");
                                listOfValues.get(listOfValues.size() - 1).add(new Value("text", text));
                            }

                        } else if (obj.containsKey("formula")) {
                            JSONObject formula = (JSONObject) obj.get("formula");
                            Object[] action = formula.keySet().toArray();
                            if (action.length != 0) {
                                switch (action[0].toString()) {
                                    case "reference":
                                        link = (String) formula.get("reference");
                                        listOfValues.get(listOfValues.size() - 1).add(new Value("reference", link));
                                        break;
                                    case "sum":
                                        JSONArray sumArr = (JSONArray) formula.get("sum");
                                        Iterator sumItr = sumArr.iterator();
                                        double sumRez = 0;
                                        while (sumItr.hasNext()) {
                                            JSONObject elementOfSum = (JSONObject) sumItr.next();
                                            if (elementOfSum.containsKey("reference")) {
                                                link = (String) elementOfSum.get("reference");
                                                int colPos = toNumber(link.substring(0, 1));
                                                int rowPos = Integer.valueOf(link.substring(1, 2));
                                                double number = (getValueOfList(listOfValues, rowPos - 1, colPos - 1)).doubleVal;
                                                sumRez += number;
                                            }
                                        }
                                        listOfValues.get(listOfValues.size() - 1).add(new Value("number", sumRez));
                                        break;
                                    case "multiply":
                                        JSONArray multiArr = (JSONArray) formula.get("multiply");
                                        Iterator multiItr = multiArr.iterator();
                                        double multiRez = 1;
                                        while (multiItr.hasNext()) {
                                            JSONObject elementOfSum = (JSONObject) multiItr.next();
                                            if (elementOfSum.containsKey("reference")) {
                                                link = (String) elementOfSum.get("reference");
                                                int colPos = toNumber(link.substring(0, 1));
                                                int rowPos = Integer.valueOf(link.substring(1, 2));
                                                double number = (getValueOfList(listOfValues, rowPos - 1, colPos - 1)).doubleVal;
                                                multiRez *= number;
                                            }
                                        }
                                        listOfValues.get(listOfValues.size() - 1).add(new Value("number", multiRez));
                                        break;
                                    case "divide":
                                        JSONArray divArr = (JSONArray) formula.get("divide");
                                        Iterator divItr = divArr.iterator();
                                        double divRez = 0;
                                        boolean flag = false;
                                        while (divItr.hasNext()) {
                                            JSONObject element = (JSONObject) divItr.next();
                                            if (element.containsKey("reference")) {
                                                link = (String) element.get("reference");
                                                int colPos = toNumber(link.substring(0, 1));
                                                int rowPos = Integer.valueOf(link.substring(1, 2));
                                                double number = (getValueOfList(listOfValues, rowPos - 1, colPos - 1)).doubleVal;
                                                if (!flag) {
                                                    divRez = number;
                                                    flag = true;
                                                } else {
                                                    divRez /= number;
                                                }
                                            }
                                        }
                                        listOfValues.get(listOfValues.size() - 1).add(new Value("number", divRez));
                                        break;
                                    case "is_greater":
                                        JSONArray greaterArr = (JSONArray) formula.get("is_greater");
                                        Iterator greaterItr = greaterArr.iterator();
                                        boolean greaterRez = false;
                                        double val1 = 0;
                                        double val2 = 0;
                                        int count = 0;
                                        while (greaterItr.hasNext()) {
                                            JSONObject element = (JSONObject) greaterItr.next();
                                            if (element.containsKey("reference")) {
                                                link = (String) element.get("reference");
                                                int colPos = toNumber(link.substring(0, 1));
                                                int rowPos = Integer.valueOf(link.substring(1, 2));
                                                double number = (getValueOfList(listOfValues, rowPos - 1, colPos - 1)).doubleVal;
                                                if (count == 0) {
                                                    val1 = number;
                                                } else {
                                                    val2 = number;
                                                }
                                                count++;
                                            }
                                        }
                                        if (val1 > val2) {
                                            greaterRez = true;
                                        }
                                        listOfValues.get(listOfValues.size() - 1).add(new Value("boolean", greaterRez));
                                        break;
                                    case "is_equal":
                                        JSONArray equalArr = (JSONArray) formula.get("is_equal");
                                        Iterator equalItr = equalArr.iterator();
                                        boolean equalResult = false;
                                        double val3 = 0;
                                        double val4 = 0;
                                        count = 0;
                                        while (equalItr.hasNext()) {
                                            JSONObject element = (JSONObject) equalItr.next();
                                            if (element.containsKey("reference")) {
                                                link = (String) element.get("reference");
                                                int colPos = toNumber(link.substring(0, 1));
                                                int rowPos = Integer.valueOf(link.substring(1, 2));
                                                double number = (getValueOfList(listOfValues, rowPos - 1, colPos - 1)).doubleVal;
                                                if (count == 0) {
                                                    val3 = number;
                                                } else {
                                                    val4 = number;
                                                }
                                                count++;
                                            }
                                        }
                                        if (val3 == val4) {
                                            equalResult = true;
                                        }
                                        listOfValues.get(listOfValues.size() - 1).add(new Value("boolean", equalResult));
                                        break;
                                    case "not":
                                        JSONObject notObject = (JSONObject) formula.get("not");
                                        boolean notResult = false;
                                        if (notObject.containsKey("reference")) {
                                            link = (String) notObject.get("reference");
                                            int colPos = toNumber(link.substring(0, 1));
                                            int rowPos = Integer.valueOf(link.substring(1, 2));
                                            boolean notVal = (getValueOfList(listOfValues, rowPos - 1, colPos - 1)).boolVal;
                                            notResult = !notVal;
                                            listOfValues.get(listOfValues.size() - 1).add(new Value("boolean", notResult));
                                        }
                                        break;
                                    case "and":
                                        JSONArray andArr = (JSONArray) formula.get("and");
                                        Iterator andItr = andArr.iterator();
                                        boolean andResult = true;
                                        boolean errorOccured = false;
                                        while (andItr.hasNext()) {
                                            JSONObject element = (JSONObject) andItr.next();
                                            if (element.containsKey("reference")) {
                                                link = (String) element.get("reference");
                                                int colPos = toNumber(link.substring(0, 1));
                                                int rowPos = Integer.valueOf(link.substring(1, 2));
                                                try {
                                                    boolean andVal = (boolean) (getValueOfList(listOfValues, rowPos - 1, colPos - 1)).getValue();
                                                    andResult = andResult && andVal;
                                                } catch (Throwable Error) {
                                                    listOfValues.get(listOfValues.size() - 1).add(new Value("error", "Incorrect value type"));
                                                    errorOccured = true;
                                                    break;
                                                }
                                            }

                                        }
                                        if (!errorOccured) {
                                            listOfValues.get(listOfValues.size() - 1).add(new Value("boolean", andResult));
                                        }

                                        break;
                                    case "or":
                                        JSONArray orArr = (JSONArray) formula.get("or");
                                        Iterator orItr = orArr.iterator();
                                        boolean orResult = true;
                                        errorOccured = false;
                                        count = 0;
                                        while (orItr.hasNext()) {
                                            JSONObject element = (JSONObject) orItr.next();
                                            if (element.containsKey("reference")) {
                                                link = (String) element.get("reference");
                                                int colPos = toNumber(link.substring(0, 1));
                                                int rowPos = Integer.valueOf(link.substring(1, 2));
                                                try {
                                                    boolean orVal = (boolean) (getValueOfList(listOfValues, rowPos - 1, colPos - 1)).getValue();
                                                    if (count == 0) {
                                                        orResult = orVal;
                                                    } else {
                                                        orResult = orResult || orVal;
                                                    }
                                                } catch (Throwable Error) {
                                                    listOfValues.get(listOfValues.size() - 1).add(new Value("error", "Incorrect value type"));
                                                    errorOccured = true;
                                                    break;
                                                }
                                            }
                                            count++;
                                        }
                                        if (!errorOccured) {
                                            listOfValues.get(listOfValues.size() - 1).add(new Value("boolean", orResult));
                                        }
                                        break;
                                    case "if":
                                        JSONArray ifArr = (JSONArray) formula.get("if");
                                        Iterator ifItr = ifArr.iterator();
                                        Value ifResult;
                                        count = 0;
                                        int count2 = 0;
                                        greaterRez = false;
                                        while (ifItr.hasNext()) {
                                            JSONObject element = (JSONObject) ifItr.next();
                                            if (element.containsKey("is_greater")) {
                                                greaterArr = (JSONArray) element.get("is_greater");
                                                greaterItr = greaterArr.iterator();
                                                val1 = 0;
                                                val2 = 0;
                                                count = 0;
                                                while (greaterItr.hasNext()) {
                                                    JSONObject el = (JSONObject) greaterItr.next();
                                                    if (el.containsKey("reference")) {
                                                        link = (String) el.get("reference");
                                                        int colPos = toNumber(link.substring(0, 1));
                                                        int rowPos = Integer.valueOf(link.substring(1, 2));
                                                        double number = (getValueOfList(listOfValues, rowPos - 1, colPos - 1)).doubleVal;
                                                        if (count == 0) {
                                                            val1 = number;
                                                        } else {
                                                            val2 = number;
                                                        }
                                                        count++;
                                                    }
                                                }
                                                if (val1 > val2) {
                                                    greaterRez = true;
                                                }
                                            } else if (element.containsKey("reference")) {
                                                count2++;
                                                link = (String) element.get("reference");
                                                int colPos = toNumber(link.substring(0, 1));
                                                int rowPos = Integer.valueOf(link.substring(1, 2));
                                                ifResult = getValueOfList(listOfValues, rowPos - 1, colPos - 1);
                                                if (greaterRez && count2 == 1) {
                                                    listOfValues.get(listOfValues.size() - 1)
                                                            .add(new Value(ifResult.typeVal, ifResult.getValue()));
                                                } else if (!greaterRez && count2 != 1) {
                                                    listOfValues.get(listOfValues.size() - 1)
                                                            .add(new Value(ifResult.typeVal, ifResult.getValue()));
                                                }
                                            }
                                        }
                                        break;
                                    case "concat":
                                        JSONArray concatArr = (JSONArray) formula.get("concat");
                                        Iterator concatItr = concatArr.iterator();
                                        String concatResult = "";
                                        while (concatItr.hasNext()) {
                                            JSONObject element = (JSONObject) concatItr.next();
                                            if (element.containsKey("value")) {
                                                JSONObject val = (JSONObject) element.get("value");
                                                String line = (String) val.get("text");
                                                concatResult = concatResult.concat(line);

                                            }
                                        }
                                        listOfValues.get(listOfValues.size() - 1).add(new Value("text", concatResult));
                                        break;
                                }
                            }

                        }

                    }

                }
                if (!data.isEmpty()) {
                    resultStr += "[";
                }
                for (int i = 0; i < listOfValues.size(); i++) {
                    for (int j = 0; j < listOfValues.get(i).size(); j++) {
                        //Value val = listOfValues.get(i).get(j);
                        Value val = getValueOfList(listOfValues, i, j);
                        if (j == 0 && i == 0) {
                            if (val.typeVal == "text") {
                                resultStr += "{ \"value\": { \"" + val.typeVal + "\": \"" + val + "\" } }";
                            } else if (val.typeVal == "error") {
                                resultStr += "{ \"error\": { \"" + val.typeVal + "\": \"" + val + "\" } }";
                            } else {
                                resultStr += "{ \"value\": { \"" + val.typeVal + "\": " + val + " } }";
                            }
                        } else if (j == 0) {
                            if (val.typeVal == "text") {
                                resultStr += "{ \"value\": { \"" + val.typeVal + "\": \"" + val + "\" } }";
                            } else if (val.typeVal == "error") {
                                resultStr += "{ \"error\": { \"" + val.typeVal + "\": \"" + val + "\" } }";
                            } else {
                                resultStr += "{ \"value\": { \"" + val.typeVal + "\": " + val + " } }";
                            }
                        } else {
                            if (val.typeVal == "text") {
                                resultStr += ",{ \"value\": { \"" + val.typeVal + "\": \"" + val + "\" } }";
                            } else if (val.typeVal == "error") {
                                resultStr += ",{ \"error\": { \"" + val.typeVal + "\": \"" + val + "\" } }";
                            } else {
                                resultStr += ",{ \"value\": { \"" + val.typeVal + "\": " + val + " } }";
                            }
                        }
                    }
                    if (i != listOfValues.size() - 1) {
                        resultStr += "],[";
                    }
                }
                if (data.isEmpty()) {
                    resultStr += "]}";
                } else {
                    resultStr += "]]}";
                }
            }

        }
        //final end
        resultStr += "]}";

        /**
         * ******************************SENDING
         * DATA****************************************
         */
        try ( CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(postURL);

            StringEntity entity = new StringEntity(resultStr);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");

            CloseableHttpResponse response = client.execute(httpPost);
            System.out.println(response.getStatusLine().getStatusCode());
            System.out.println(EntityUtils.toString(response.getEntity()));
        }
    }

    public static Value getValueOfList(List<ArrayList<Value>> list, int i, int j) {
        Value val = list.get(i).get(j);
        if ("reference" == val.typeVal) {
            int colPos = toNumber(val.ref.substring(0, 1));
            int rowPos = Integer.valueOf(val.ref.substring(1, 2));
            return getValueOfList(list, rowPos - 1, colPos - 1);
        } else {
            return val;
        }
    }

    public static int toNumber(String name) {
        int number = 0;
        for (int i = 0; i < name.length(); i++) {
            number = number * 26 + (name.charAt(i) - ('A' - 1));
        }
        return number;
    }
}
