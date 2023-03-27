package ru.hse.jade.sample.agents;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class VisitorAgent extends Agent {
    // Список доступных блюд
    private List<JSONObject> menuDishes = new ArrayList<>();

    // Генератор случайных чисел
    private Random random = new Random();
    private int timeLimit; // время, за которое нужно готовить блюда
    private AID restaurantManager;
    private ArrayList<String> menuItems;

    protected void setup() {

        // Получаем параметры агента из аргументов запуска
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            timeLimit = Integer.parseInt((String) args[0]);
        } else {
            System.out.println("Не указано время ожидания для приготовления блюд!");
            doDelete();
            return;
        }

        // Находим агента-управляющего рестораном
        restaurantManager = new AID("RestaurantManagerAgent", AID.ISLOCALNAME);

        // Отправляем запрос на получение части меню
        ACLMessage menuRequest = new ACLMessage(ACLMessage.REQUEST);
        menuRequest.addReceiver(restaurantManager);
        JSONObject requestContent = new JSONObject();
        requestContent.put("type", "menuPartRequest");
        requestContent.put("timeLimit", timeLimit);
        menuRequest.setContent(requestContent.toJSONString());
        send(menuRequest);

        // Создаем TickerBehaviour для получения сообщений
        addBehaviour(new TickerBehaviour(this, 1000) {
            protected void onTick() {
                ACLMessage msg = myAgent.receive();
                if (msg != null) {
                    if (msg.getSender().equals(restaurantManager)) {
                        processMessage(msg);
                    }
                } else {
                    block();
                }
            }
        });
    }

    protected void takeDown() {
        System.out.println("Посетитель " + getAID().getName() + " закончил посещение ресторана.");
    }

    private void processMessage(ACLMessage msg) {
        String content = msg.getContent();
        JSONParser parser = new JSONParser();
        JSONObject responseContent;
        try {
            responseContent = (JSONObject) parser.parse(content);
            String responseType = (String) responseContent.get("type");
            if (responseType.equals("menuPartResponse")) {
                JSONArray menuItemsJson = (JSONArray) responseContent.get("menuItems");
                makeOrder(menuItemsJson, timeLimit);
                menuItems = new ArrayList<String>();
                for (Object item : menuItemsJson) {
                    JSONObject menuItemJson = (JSONObject) item;
                    String name = (String) menuItemJson.get("menu_dish_name");
                    int timeToCook = ((Long) menuItemJson.get("menu_dish_time")).intValue();
                    int price = ((Long) menuItemJson.get("menu_dish_price")).intValue();
                    if (timeToCook <= timeLimit) {
                        menuItems.add(name);
                        System.out.println(name + " - " + price + " руб. (" + timeToCook + " мин.)");
                    }
                }
            } else if (responseType.equals("orderConfirmation")) {
                System.out.println("Заказ подтвержден! Можете забрать его в окошке выдачи.");
            }
        } catch (ParseException e) {
            System.out.println("Ошибка парсинга JSON: " + e.getMessage());
        }
    }


    // Метод для совершения случайного заказа
    private void makeOrder(JSONArray menu, int timeLimit) {
        // Список доступных блюд, которые можно заказать
        List<JSONObject> availableDishes = new ArrayList<>();

        // Проходимся по каждому блюду в меню
        for (Object obj : menu) {
            if (obj instanceof JSONObject) {
                JSONObject dish = (JSONObject) obj;

                // Проверяем, что блюдо можно приготовить за указанное время
                int dishTime = ((Long) dish.get("menu_dish_time")).intValue();
                if (dishTime <= timeLimit) {
                    // Если блюдо можно приготовить, то добавляем его в список доступных блюд
                    availableDishes.add(dish);
                }
            }
        }

        // Если нет доступных блюд, выводим сообщение об этом и завершаем выполнение метода
        if (availableDishes.isEmpty()) {
            System.out.println("Извините, в нашем меню нет блюд, которые можно приготовить за указанное время");
            return;
        }

        // Выбираем случайное блюдо из списка доступных блюд
        Random random = new Random();
        JSONObject chosenDish = availableDishes.get(random.nextInt(availableDishes.size()));

        // Составляем список заказанных блюд
        JSONArray orderedDishes = new JSONArray();
        orderedDishes.add(chosenDish.get("menu_dish_name"));

        // Передаем список заказанных блюд агенту управляющего
        ACLMessage orderRequest = new ACLMessage(ACLMessage.REQUEST);
        orderRequest.addReceiver(restaurantManager);
        orderRequest.setContent(orderedDishes.toJSONString());
        send(orderRequest);

        // Выводим сообщение о том, что заказ был успешно оформлен
        System.out.println("Ваш заказ: " + chosenDish.get("menu_dish_name") +
                ", время приготовления: " + chosenDish.get("menu_dish_time") +
                " минут, цена: " + chosenDish.get("menu_dish_price") + " рублей");
    }

}
