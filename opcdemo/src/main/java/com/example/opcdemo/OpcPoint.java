package com.example.opcdemo;

public class OpcPoint {  private String sourceFile;
    private String itemId;



    public OpcPoint() {
    }

    public OpcPoint(String sourceFile,  String itemId) {
        this.sourceFile = sourceFile;
        this.itemId = itemId;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }


    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    @Override
    public String toString() {
        return "OpcPoint{" +
                "sourceFile='" + sourceFile + '\'' +
                ", itemId='" + itemId + '\'' +
                '}';
    }
}