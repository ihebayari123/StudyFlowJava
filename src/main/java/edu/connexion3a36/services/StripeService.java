package edu.connexion3a36.services;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import edu.connexion3a36.entities.CartItem;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class StripeService {

    private static final String API_KEY;

    static {
        try (InputStream input = StripeService.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            Properties props = new Properties();
            props.load(input);
            API_KEY = props.getProperty("stripe.api.key");
        } catch (Exception e) {
            throw new RuntimeException("❌ Impossible de charger config.properties : " + e.getMessage());
        }
    }

    public static String creerSession(String email, List<CartItem> items) throws Exception {
        Stripe.apiKey = API_KEY;

        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
        for (CartItem item : items) {
            lineItems.add(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity((long) item.getQuantite())
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("eur")
                                            .setUnitAmount((long) (item.getProduit().getPrix() * 100))
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName(item.getProduit().getNom())
                                                            .build()
                                            )
                                            .build()
                            )
                            .build()
            );
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(email)
                .setSuccessUrl("https://google.com")
                .setCancelUrl("https://google.com")
                .addAllLineItem(lineItems)
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }
}