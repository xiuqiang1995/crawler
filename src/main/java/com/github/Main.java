package com.github;

import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

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

public class Main {
    public static void main(String[] args) throws IOException {
        //待处理链接池
        List<String> linkPool = new ArrayList<>();
        //已处理链接池
        Set<String> processedLinks = new HashSet<>();

        linkPool.add("https://sina.cn");
        while (true) {
            if (linkPool.isEmpty()) {
                break;
            }
            String link = linkPool.remove(linkPool.size() - 1);
            if (processedLinks.contains(link)) {
                continue;
            }
            if (isNewsLink(link)) {
                Document doc = HttpGetAndParseHtml(link);
                doc.select("a").stream().map(aTag -> aTag.attr("href")).forEach(linkPool::add);
                storeIntoDataBase(doc);
                processedLinks.add(link);
            }
        }
    }

    private static void storeIntoDataBase(Document doc) {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            String title = articleTags.get(0).child(0).text();
            System.out.println("title = " + title);
        }
    }

    private static Document HttpGetAndParseHtml(String link) throws IOException {
        //从官方文档抄代码 http://hc.apache.org/httpcomponents-client-4.5.x/quickstart.html
        CloseableHttpClient httpclient = HttpClients.createDefault();
        if (link.startsWith("//")) {
            link = "https:" + link;
        }
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
        Document doc = null;
        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            System.out.println(response1.getStatusLine());
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
