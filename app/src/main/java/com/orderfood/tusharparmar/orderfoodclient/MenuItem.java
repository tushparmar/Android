package com.orderfood.tusharparmar.orderfoodclient;

/**
 * Created by Tushar Parmar on 2/20/2018.
 */

public class MenuItem {
    private String name, desc, imageURL, price;
    public MenuItem() {
    }

    public MenuItem(String name, String desc, String image, String price) {
        this.name = name;
        this.desc = desc;
        this.imageURL = imageURL;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }
}
