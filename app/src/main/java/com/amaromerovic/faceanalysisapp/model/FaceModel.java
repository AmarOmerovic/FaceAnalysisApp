package com.amaromerovic.faceanalysisapp.model;

public class FaceModel {
    private final int id;
    private final String text;

    public FaceModel(int id, String text) {
        this.id = id;
        this.text = text;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }
}
