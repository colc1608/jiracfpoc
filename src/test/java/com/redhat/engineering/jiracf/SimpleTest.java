package com.redhat.engineering.jiracf;

import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;

public class SimpleTest {

    private String generateRandomString() {
        int ranLength = ThreadLocalRandom.current().nextInt(1, 100);
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= ranLength; i++) {
            int index = ThreadLocalRandom.current().nextInt(97, 123);
            builder.append((char) index);
        }
        return builder.toString();
    }

    @Test
    public void test(){
        for(int i=0;i<10; i++){
            System.out.println(generateRandomString());
        }
    }
}
