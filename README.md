#PreSense Android Application

This is the repository for the PreSense Android Application. 

The main PreSense repository can be found here : https://github.com/chaychoong/presense

###Running the app

* Ensure you are in the region of a transmitting Estimote Beacon with a UUID of B9407F30-F5F8-466E-AFF9-25556B57FE6D

* Set your username. This may not necessarily be your Slack username, but the name that others will use to check on you! (Note: It is case sensitive!)

* Use this as the Webhook URL: https://[heroku app name].herokuapp.com/hubot/notify/general

* Hit Register and you are done! Your status will automatically be set as **Available**. You can toggle your status between **Available** and **Busy**.

* When you go out of range of your beacon, your status will be set to "out of offce". During this state, you will not be able to toggle your status.

* When you are back in range of your beacon, your status will be reset to **Available**

Enjoy!

* Note: for Android users, for your status to be automatically changed when you are enter or exit your beacon range, the app should be running in the background in the "Recent Apps" tab. If you force stop the app or swipe it close, the background monitoring will not update the bot until you open up the app again.

