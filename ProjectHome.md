# Android social application. #


## Description ##
Application was developed as part of semesterial project at the **Technion** Laboratory of Computer Communication and Networking http://www.cs.technion.ac.il/Labs/Lccn/.

## Android Market ##
PubDates https://market.android.com/search?q=pubdates&so=1&c=apps

## Goal ##
Main goal of this project is to develop android application, which leverage MANET architecture in order to setup and initiate **Ad Hoc Social Network**.

In the current level application aims to implement next user scenarios.


## User Scenarios ##

  1. ### Dating. ###
    * User get bored at pub and would like to find an some interesting person(could be more than one) to be able to chat with him and probably later meet.
    * User opens the application and create account with his personal information.
    * After account has been created he performs login;
    * If there is already exists someone who tried previously establish **Ad Hoc Social Network** user account will join it, otherwise will create his own network.
> > 8 After user joins already existing network he will be able to list available users in the system and chat with them, more over he will be able to exchange personal information with each other.
  1. ### Disaster management. ###
    * There is a disaster (fire, earthquake etc.) which has to be handled.
    * Special rescue team arrives to the area.
    * In order to coordinates rescue activities team setup an ad hoc mobile network using our application.
    * By leveraging application capabilities team member manage to communicate and perform better coordination.
  1. ### Meeting new people in the campus. ###
    * New student arrive to university campus.
    * Enter his faculty and open the application.
    * Application connects to the ad hoc community created especially for "newcommers" in order to help them to communicate and meet each other.
    * There also several students from advanced semester connected to the community in order to help and advice "newcommers"

Application covers explained scenario as well capable to re-establish network connectivity on network faults.

## Design/Assumptions ##

Application designed with next assumption: there is exists at least one node which is able to see other nodes. This node is chosen to be a network leader which will have to route communication among different clients.


## Supervisors ##

Project was developed under supervision of:

Mr. **Itai Dabran**

Prof. **Dany Raz**

Sponsored by:

**Qualcom.**


## Documentation ##

Additional documentation could be found here: http://code.google.com/p/androidsocial/downloads/list

## Instructions for deployment and installation ##

  * In order to be able to work with application the Android device has to be unlocked to work in superuser mode, since there is a need to setup and initialize DHCP server.
  * After you ensure that device has been unlocked you need to download the application from Android Market and install it.
  * To see complete and full functionality of application you need to have preferably 2+ devices so one will work as a communication server while second one will work as a regular client.
  * After you have finished the installation you need to provide you picture and common data describing yourself.
  * Enjoy!

## Pictures ##
  * http://www.flickr.com/photos/48040818@N07/5687543129/
  * http://www.flickr.com/photos/48040818@N07/5687543273/
  * http://flic.kr/p/9ED4jY