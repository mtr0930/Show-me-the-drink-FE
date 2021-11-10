package com.example.Drinks_Classification;
import com.google.gson.annotations.SerializedName;

public class drinks {
    @SerializedName("name")
    private String name;
    @SerializedName("type")
    private String type;
    @SerializedName("flavor")
    private String flavor;
    @SerializedName("cautions")
    private String cautions;
    public String getName(){
        return name;
    }
    public String getType(){
        return type;
    }
    public String getFlavor(){
        return flavor;
    }
    public String getCautions(){
        return cautions;
    }

}
