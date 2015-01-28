#!/usr/bin/env node
(function(){
    var PORT = 9000,
        express = require("express"),
        server = express();

    console.log("############################");
    console.log("Starting server on port " + PORT);
    console.log("############################");

    var players = {};

    server.listen(PORT);

    server.get("/hello", function(req, res){
        res.set("Access-Control-Allow-Origin", "*")
            .json({hello: "I'm OK!"})
    });

    server.put("/player/:nick", function(req, res) {
        var nick = req.params.nick;
        console.log("PUT: /player/"+nick);

        res.set("Access-Control-Allow-Origin", "*");
        if (players[nick]) {
            res.status(409)
                .send({error: "Nick '"+nick+"' already registered"})
        } else {
            var playerData = {
                "quests": ["seealltalks", "networkingchanp"],
                "preferences": {"drink": "Wine", "eat": "Meat"},
                "id": generateRandomId(),
                "nick": nick
            };
            players[nick] = playerData;
            res.status(201)
            setTimeout(function(){
                res.send(playerData);
            }, 2000);
        }
    });


    var generateRandomId = function(){
        return Math.random().toString(36).replace(/[^a-z0-9]+/g, '').substr(0, 5);
    }
}());