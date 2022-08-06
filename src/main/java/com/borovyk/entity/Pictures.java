package com.borovyk.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Pictures(@JsonProperty("photos") List<Picture> pictures) {}
