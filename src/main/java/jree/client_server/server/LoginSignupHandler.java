package jree.client_server.server;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.interfaces.Verification;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import jree.abs.utils.StaticFunctions;
import org.bson.Document;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

public class LoginSignupHandler {

    private final MongoCollection<Document> users;
    private final MessageDigest sha256;
    private final Algorithm jwtAlgorithm = Algorithm.HMAC512("some_secret");

    public LoginSignupHandler(MongoDatabase database) {
        users = database.getCollection("users");
        users.createIndex(Indexes.ascending("username"), new IndexOptions().unique(true));
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private synchronized String hash(String input) {
        return Base64.getEncoder().encodeToString(sha256.digest(input.getBytes(StandardCharsets.UTF_8)));
    }

    public void signup(String username, String password, Supplier<Long> clientIdProvider) throws LoginSignupException {
        InsertOneResult result = users.insertOne(
                new Document()
                .append("username", hash(username))
                .append("password", hash(password))
        );

        if(result.getInsertedId()==null)
            throw LoginSignupException._500;

        UpdateResult updateResult = users.updateOne(eq("username", hash(username)), set("clientId", clientIdProvider.get()));

        if(updateResult.getMatchedCount()<0)
            throw LoginSignupException._500;

    }

    public String login(String username, String password, Function<Long, Long> sessionBuilder) throws LoginSignupException {
        Document document = users.find(and(eq("username", hash(username)),
                eq("password", hash(password))))
                .first();

        System.out.println(document);

        if(document==null)
            throw LoginSignupException._401;

        long clientId = document.getLong("clientId");
        long sessionId = sessionBuilder.apply(clientId);

        String token = JWT.create().withIssuer("jree-chat-server")
                .withClaim("clientId", clientId)
                .withClaim("sessionId", sessionId)
                .sign(jwtAlgorithm);
        return token;
    }

    public Long[] verifyToken(String token) {
        JWTVerifier verifier = JWT.require(jwtAlgorithm).build();
        DecodedJWT jwt = verifier.verify(token);
        Claim clientId = jwt.getClaim("clientId");
        Claim sessionId = jwt.getClaim("sessionId");
        return new Long[]{clientId.asLong(), sessionId.asLong()};
    }


}
