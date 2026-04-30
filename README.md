This is a Java [Spring Boot] (https://spring.io/projects/spring-boot) example project, showing how to implement and 
execute trading strategies using the technical analysis framework [TA4J] (https://github.com/ta4j/ta4j).

In the current state, only USDM futures markets on Binance, using the Binance websocket and REST API are supported.
The future goals of this project are to abstract the API allowing to implement against any Broker and also provide easy
ways of adding custom strategies.

DISCLAIMER: The intention of this project is **not** to provide a fully functional trading bot. This project ist for
educational purposes only. None of the strategies provided with this project are guaranteed to make profit, and the
implementation is not guaranteed to be secure and free of bugs. Trading comes with high risk. I do not recommend using
this project or even parts of if for trading real assets with real money. Most brokers provide demo accounts, where
you can try out all the features in a simulated risk-free environment. For instance on Binance you can use: 
https://demo.binance.com/. Although this project currently builds upon the Binance API, this is no recommendation for
using Binance in general and Binance itself is not involved in this project at all. The reasons I've chosen Binance
for this purpose are, that they provide a public API, that is well enough documented and a working, stable demo 
account.

# What it does
On startup, all strategies defined in `application.properties` will be loaded. A klines-provider will be created for
each strategy, reading and providing the marked data for the symbol and timeframe the strategy requires. When any
klines-provider notifies a price update, the strategy connected to this provider will check its entry or exit 
conditions. If an entry condition matches, a new position will be opened using the orders API of connected broker.
This position will be tracked, until the strategy matches an exit condition - or if the position's stop loss price was 
reached. The position will then be stored in database. Everytime a position is opened or closed a message will be 
using a Telegram bot.

# Configuration
To get started quickly, just provide the environment variables defined inf `deployment/template.env`. Provide the
name of your Telegram bot without the `@`-prefix, e.g. `TA4J_TG_BOT_NAME: my-tg-bot`. You can create a Binance-API key
in the demo account: https://demo.binance.com/de/my/settings/api-management - ofc you have to have a real 
Binance account for using the demo features, but you don't have to complete a KYC process or even deposit money just
doing so.

This project works with any MySQL 8 database, but I recommend using a docker container, as defined in
`deployment/local/docker-compose.yml`.

## MySQL
As being said, you can use any MySQL 8 database. Instead of an initialization script, we currently use the auto-update
feature of hibernate, as you can see in the `application.properties`: `spring.jpa.hibernate.ddl-auto=update`.
If you are using an already existing database instance, providing the root user password with the environment variable
`TA4J_MYSQL_ROOT` is not required. Just make sure that the user provided with the environment variable `TA4J_MYSQL_USER`
exists and is authorized to create and alter tables as well as reading, updating and deleting from them.

Using the docker-compose file `deployment/local/docker-compose.yml`, providing a root password with the environment 
variable `TA4J_MYSQL_ROOT` is mandatory. Just execute `docker compose up -d` and the database will be setup 
correctly and ready to use.

## Binance-API
DISCLAIMER (again): do not use any real API key here. Using anything provided with this project for real trading exposes
you to high risk of loosing money.

Create an account at https://www.binance.com, log in, hover 
**Trade** in the top menu and click **Demo Trading**. 

![binance-demo-trading.png](readme/img/binance-demo-trading.png)


Then hover your account icon in the top right menu and click Demo Trading API.

![binance-demo-trading-api.png](readme/img/binance-demo-trading-api.png)


Click on the button **Create API** in the top right, and choose **System generated** API Key type. Enter a label for
your API key, click next, and it should look like this:

![api-key.png](readme/img/api-key.png)

The environment variable `BINANCE_API_KEY` then refers to `API Key` and `BINANCE_API_SECRET` then refers to
`Secret Key`.

## Telegram Bot
Using the Telegram bot is optional and currently only active in the `prod` profile. In any other profile, any messages
will just be printed out to the command line. To create a new Telegram bot, open the Telegram app and search in `apps` 
for `BotFather`.

![tg_botfather.jpg](readme/img/tg_botfather.jpg)

Use the BotFather to create a new bot. Give it a Bot Name (this is the display name only), and choose a unique 
username (this has to start with `t.me/`).

![tg_new_bot.jpg](readme/img/tg_new_bot.jpg)

Create the bot and you should be provided a key. Now for the environment variables:
`TA4J_TG_BOT_NAME` would be the bot username - in this example: `hello_world_2999_bot` and `TA4J_TG_BOT_PW` is the
key, that is displayed if you tab revoke in the screen shown below. 

![tg_hello_bot.jpg](readme/img/tg_hello_bot.jpg)

**Note** that this is not a very safe way of communication. Everyone who knows the name of your bot, can subscribe it
and read everything, this bot broadcasts. Here, we only send position data and error messages - nothing anybody could
use for anything. But be aware of that, if you extend the message API. Never send any personal information, passwords
or keys with the bot API.

## Application properties
There are some more options in the `src/main/resources/application.properties`. The environment variables explained
above may be referenced there, too. Make sure your JVM has access to the environment variables (this may more be an
issue on Unix based systems). Every property is documented in the file itself, so there is not much need to explain
them here.

# Startup
You can start the application using your IDE, for instance with IntelliJ create a run configuration like this:
![intellij_run_config.png](readme/img/intellij_run_config.png)

Well, because this is a Java application, you can use plain old java CLI as well.

Or you can use the configuration provided in `deployment/prod` to run everything in a docker environment. 


