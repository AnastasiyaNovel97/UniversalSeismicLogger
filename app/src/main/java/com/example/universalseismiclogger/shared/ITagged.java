package com.example.universalseismiclogger.shared;

public interface ITagged {
    default String getTag() {return  this.getClass().getSimpleName();}
}
