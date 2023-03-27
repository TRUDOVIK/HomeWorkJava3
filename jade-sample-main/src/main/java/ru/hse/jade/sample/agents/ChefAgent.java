package ru.hse.jade.sample.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ChefAgent extends Agent {
    private final int COOKING_DELAY = 1000; // задержка при приготовлении каждого блюда (в миллисекундах)

    @Override
    protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    // Получаем заказ в формате JSON
                    String orderJson = msg.getContent();

                    // Парсим JSON
                    JSONParser parser = new JSONParser();
                    try {
                        JSONObject order = (JSONObject) parser.parse(orderJson);
                        JSONArray items = (JSONArray) order.get("items");

                        // Вычисляем время, необходимое на приготовление всех блюд
                        int totalTime = 0;
                        for (Object item : items) {
                            JSONObject dish = (JSONObject) item;
                            int cookTime = Integer.parseInt(dish.get("cook_time").toString());
                            totalTime += cookTime;
                        }

                        // Готовим блюда
                        Thread.sleep(totalTime * COOKING_DELAY);

                        // Выводим сообщение о готовности заказа
                        System.out.println("Заказ готов. Вы можете забрать его в окошке выдачи.");

                    } catch (ParseException | InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    block();
                }
            }
        });
    }
}
