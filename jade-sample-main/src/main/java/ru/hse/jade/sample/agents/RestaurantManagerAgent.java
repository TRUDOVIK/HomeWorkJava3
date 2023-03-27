package ru.hse.jade.sample.agents;

import java.io.FileReader;
import java.io.IOException;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.json.simple  .JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import ru.hse.jade.sample.configuration.JadeAgent;

@JadeAgent
public class RestaurantManagerAgent extends Agent {

    private static final String MENU_FILE_PATH = "../../../../../../../menu.json";

    private JSONArray menuDishes;

    protected void setup() {
        System.out.println("Hello, I am the Restaurant Manager Agent!");

        // Load menu from file
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(MENU_FILE_PATH));
            JSONObject menu = (JSONObject) obj;
            menuDishes = (JSONArray) menu.get("menu_dishes");
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            doDelete();
        }

        // Add behaviour to handle requests from visitor
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    String content = msg.getContent();
                    long maxPreparationTime = Long.parseLong(content);

                    // Filter menu dishes based on max preparation time
                    JSONArray availableDishes = new JSONArray();
                    for (Object dishObj : menuDishes) {
                        JSONObject dish = (JSONObject) dishObj;
                        long preparationTime = (long) dish.get("menu_dish_preparation_time");
                        if (preparationTime <= maxPreparationTime) {
                            availableDishes.add(dish);
                        }
                    }

                    // Send available dishes to visitor
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(availableDishes.toJSONString());
                    send(reply);
                } else {
                    block();
                }
            }
        });

        // Add behaviour to handle orders from visitor
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    // Получаем содержимое сообщения с заказом
                    String content = msg.getContent();
                    // Отправляем заказ повару
                    ACLMessage order = new ACLMessage(ACLMessage.REQUEST);
                    order.addReceiver(new AID("ChefAgent", AID.ISLOCALNAME));
                    order.setContent(content);
                    send(order);
                } else {
                    block();
                }
            }
        });
    }
}
