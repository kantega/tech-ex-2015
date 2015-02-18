#!/usr/bin/env node
(function(){
    var PORT = 9000,
        express = require("express"),
        server = express();

    console.log("############################");
    console.log("Starting server on port " + PORT);
    console.log("############################");

    server.listen(PORT);

    var players = {};

    var quests = [{
        "id": 1,
        "title": "Se all the talks",
        "desc": "A very long description that should either wrap on separate lines or be clipped at a certain length.",
        "visibility": "public",
        "achievements":  [{
            "id": 1,
            "title": "Damn thing keeps ringing",
            "desc": "Left session for 5-10 min with out going to toilet or coffee stand.",
            "visibility": "public",
            "achieved": false,
            "achievedBy": ["nick1", "nick2", "dick"]
        }]
    },
        {
            "id": 2,
            "title": "Common secret quest",
            "desc": "Lorem ipsum",
            "visibility": "secret",
            "achievements": [
            ]
        },
        {
            "id": 3,
            "title": "My private quest",
            "desc": "Lorem ipsum",
            "visibility": "private",
            "achievements": [
            ]
        }];


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
            var playerId = generateRandomId();
            var playerData = {
                "quests": ["seealltalks", "networkingchanp"],
                "preferences": {"drink": "Wine", "eat": "Meat"},
                "id": playerId,
                "nick": nick
            };
            players[playerId] = playerData;
            res.status(201);
            setTimeout(function(){
                res.send(playerData);
            }, 2000);
        }
    });

    server.get("/quests/player/:playerId", function(req, res){
        var playerId = req.params.playerId;
        console.log("GET: /quests/"+playerId);
        res.json(quests);
    });

    var generateRandomId = function(){
        return Math.random().toString(36).replace(/[^a-z0-9]+/g, '').substr(0, 5);
    }
}());