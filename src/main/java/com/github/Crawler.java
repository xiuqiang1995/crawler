package com.github;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.stream.Collectors;


public class Crawler extends Thread {
    private CrawlerDao dao;

    public Crawler(CrawlerDao dao) {
        this.dao = dao;
    }

    @Override
    public void run() {
        String link;
        try {
            //从数据库加载一个待处理link
            while ((link = dao.getOneLinkAndDeleteIt()) != null) {
                //断点续传
                if (!dao.isLinkProcessed(link)) {
                    if (isNewsLink(link)) {
//                        System.out.println("link = " + link);
                        Document doc = HttpGetAndParseHtml(link);
                        parseLinkAndStoreIntoDataBase(doc);
                        storeIntoDataBase(doc, link);
                        dao.insertProcessedLink(link);
                    }
                }

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void parseLinkAndStoreIntoDataBase(Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            if (href.startsWith("//")) {
                href = "https:" + href;
//                System.out.println("href2 = " + href);
            }
            //将爬到的LINK存入 LINKS_TO_BE_PROCESSED 表中
            if (!href.toLowerCase().startsWith("javascript")) {
                dao.insertLinkToBeProcessed(href);
            }
        }
    }


    private void storeIntoDataBase(Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            String title = articleTags.get(0).child(0).text();
//            System.out.println("title = " + title);
            String content = articleTags.get(0).select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
            dao.insertNewsIntoDatabase(link, title, content);

        }
    }


    private static Document HttpGetAndParseHtml(String link) throws IOException {
        //从官方文档抄代码 http://hc.apache.org/httpcomponents-client-4.5.x/quickstart.html
        CloseableHttpClient httpclient = HttpClients.createDefault();
        if (link.startsWith("//")) {
            link = "https:" + link;
            System.out.println("href1 ------------------ = " + link);
        }
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
        Document doc = null;
        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            HttpEntity entity1 = response1.getEntity();
            String html = EntityUtils.toString(entity1);
            doc = Jsoup.parse(html);

        }
        return doc;
    }

    private static boolean isNewsLink(String link) {
        //is Index Page ?
        if ("https://sina.cn".equals(link)) {
            return true;
        }
        //is news page and is not login page
        return link.contains("news.sina.cn") && !link.contains("passport.sina.cn");
    }
}
