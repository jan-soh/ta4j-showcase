package de.jansoh.rsistrategy.service;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Primary
@Profile({"default", "local", "dev"})
public class TestMessageService implements MessageService {

    @Override
    public void broadcast(String text) {
        System.out.println("MESSAGE SERVICE: " + text);
    }
}
