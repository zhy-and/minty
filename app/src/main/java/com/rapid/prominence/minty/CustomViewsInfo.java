package com.rapid.prominence.minty;


import java.io.Serializable;

public class CustomViewsInfo  implements Serializable {

        private String info;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    private String title;


        public CustomViewsInfo(String info,String title) {
            this.info = info;
            this.title = title;
        }

    }

