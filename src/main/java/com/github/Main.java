package com.github;

public class Main {
    public static void main(String[] args) {
        CrawlerDao dao = new MybatisCrawlerDao();
        for (int i = 0; i < 128; i++) {
            new Crawler(dao).start();
        }
    }
}
