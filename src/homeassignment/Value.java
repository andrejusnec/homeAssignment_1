package homeassignment;

public class Value {
    boolean boolVal;
    double doubleVal;
    String strVal;
    String typeVal;
    String ref;
    
    
    public Value(String type, Object val) {
        if(type == "number") {
            doubleVal = (double) val;
        } else if(type == "text") {
            strVal = (String) val;
        } else if(type == "boolean"){
            boolVal = (boolean) val;
        } else if(type == "error") {
            strVal = (String) val;
        } else if(type == "reference"){
            ref = (String) val;
        } 
        typeVal = type;
              
    }
     public Object getValue() {
         if(typeVal == "number") {
            return doubleVal;
        } else if(typeVal == "text") {
            return strVal;
        } else if(typeVal == "boolean"){
            return boolVal;
        }else if(typeVal =="error") {
            return strVal;
        } else if(typeVal == "reference") {
            return ref;
        }else {
            return null;
        }
     }
     public String toString() {
         if(typeVal == "number") {
            
            return String.format("%f", doubleVal);
        } else if(typeVal == "text") {
            return strVal;
        } else if(typeVal == "boolean"){
            return String.format("%b", boolVal);
        }else if(typeVal == "error") {
            return strVal;
        } else if(typeVal == "reference"){
            return ref;
        }
        else {
            return null;
        }
     }
}
