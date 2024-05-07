---
---

# Authentication

The authentication module adds authentication and authorization capabilities to the BCO system. It provides functions to set user settings, like a password, to login as a user and to check permissions needed to perform an action.

## Table Of Contents

- [Integration](#integration): How does this module integrate into the workflow of BCO?
- [User Registration](#user-registration): How do you register a user?
- [User Authentication](#user-authentication): How do you login with a user?
- [User Authorization](#user-authorization): How does authorization work?
- [Accessing User Settings](#accessing-user-settings): How do you see or change settings of a user?
- [Development of Authenticated Services](#development-of-authenticated-services): How to integrate authentication into a server side application?

## Integration

This section on integration gives an overview of how the authentication module is working together with the other BCO modules. This section is also an introduction to this module.

The following figure shows the communication performed between this module and others. 

![Architecture](/img/bco/authentication/architecture.svg)

The arrows indicate the direction communication takes places. The authentication module basically communicates with the [Registry](https://github.com/openbase/bco.registry), [DAL](https://github.com/openbase/bco.dal) and any client application (in this case [BCozy](https://github.com/openbase/bco.bcozy)). 

The links seen on the bottom right indicate requests from the client application that include [registering a user](#user-registration), [logging in a user](#user-authentication), and [accessing a user's settings](#accessing-user-settings). Heads up: these are the only actions client applications will perform with this module.

It is important to clarify what *accessing user settings* is about. This module is only responsible for settings regarding the authentication. These are the password and the admin flag of a user. Other data like the username or the email address are stored by the `Registry`. For changing such data you must access the Registry directly via the very top links from BCozy to the Registry.

Now let's consider the links from `BCozy` to `DAL`. These links are used whenever you you want to perform an action like switching on a light bulb in your apartment. In this case the [SessionManager](#the-session-manager) of this module is your best friend. Once you [logged in](#how-to-login-a-user) using the `SessionManager` the authentication and authorization of the action is done automatically in the background. You don't need to worry about that yourself anymore. 

Lastly consider the links from the Registry and DAL to the authentication module. These links should not bother you as well. They are used to perform authentication on the server side. Both of these components perform a login as a special user and request a key from the AuthenticatorController. This way, whenever you perform an action or try to change entries in the Registry the `Registry` or `DAL` are able to test if you are authenticated with a valid session. Your request to perform such an action will be permitted if your session is valid and if your user has permissions for it. If not, you won't be able to perform such action and an appropriate exception is returned. For a more detailed explanation have a look at [development of authenticated services](#development-of-authenticated-services).

We hope this small introduction to the module helped you to get a rough overview and makes you more comfortable using it. We tried to hide the authentication and authorization process behind the curtains as much as possible. All you have to worry about is using the [SessionManager](#the-session-manager) to [register a user](#user-registration), [login a user](#user-authentication), and [accessing a user's settings](#accessing-user-settings). 

## User Registration
For the system it is necessary to have at least one user to operate it. Therefore the system will register a default administrator on its first start. This default administrator is able to add new user to the system. This section will explain how this is done. But before that an important differentiation between users and clients is amplified.

### Users and Clients

BCO serves the requests of a user. Imagine a user wants to turn on a light bulb. To do this he sends a request to BCO which in turn checks his permissions and turns on the light if his permissions qualify him to do so. To BCO there is no difference between a user and a client. But the client side of the system differentiates between those two. 

Users are specified as physical users like you and me. We can login and perform actions that we have the right permissions for. The permissions of a user are managed by the system administrators.
   
Among those users are also clients that are identified as the hosts of client-side-software-applications like the machine running bcozy or bcomfy. This can be a touch display hanging on a wall with which everybody can control your apartment. A system administrator can give a client as many permissions as he can give a user, although this is not recommended, as normally everybody can control a client.

### The Registration Process

Upon the first start of BCO an initial user with administrator rights is automatically registered by the system. Its credentials are as follows:
```
username: admin
password: admin
```
This account can be used to register new users and appoint regular users administrators. These regular users can be his family, friends, flat mates and so on. Any administrator can appoint a regular user an administrator.

#### The Session Manager

Before explaining how the actual registration works, you need to know what a `SessionManager` is. It is a class that offers all kinds of methods that allow to:

- register a user/client
- login and logout
- delete a user/client
- change the password of a user
- appoint a user an administrator or do the opposite

**The Session Manager will not be instantiated by yourself.** You will have to call `SessionManager.getInstance()` to get an instance of it. The instance is shared on the client application. The `SessionManager` will help you keep your session variables so that you do not have to bother about that.

We mentioned before that when performing an action, like turning on a light bulb, a user must have the right permissions to do so. The Session Manager ensures that all necessary user information is transferred to BCO automatically so that authentication and authorization can be performed. To make this working the overall system needs to have at least one registered user e.g. the default administrator account.

#### Registration via the Administrator Account

**A user or a client cannot register itself.** Either can only be registered by an administrator. In the case of user registration an administrator has to log in and access a formula where he can input both a userId and a preliminary password for the user. Afterwards the user can login and perform actions that he has permissions for. The registration process of a user and a client slightly differ. How they differ is clarified in both the following sections.

:::info INFO
Registration of new users can only be done by an administrator because this way user data like the password can be encrypted. Therefore it is always necessary to have at least one administrator in the system.
:::

#### Registration of a User

As mentioned above an administrator has to login and access some kind of formula to input the `userId`, `password` and `isAdmin` flag. While the first two inputs are self explanatory the third `isAdmin` determines if the user should be appointed an administrator. To register the user simply call the method `registerUser(userId, password, isAdmin)` on your `SessionManager`-instance. The following code block gives a rough understanding of the procedure.
```
// retrieve the SessionManager
SessionManager sessionManager = SessionManager.getInstance();

// login the administrator
sessionManager.login(adminId, adminPassword); // omit if already logged in

// register the user
sessionManager.registerUser(userId, userPassword, isAdmin);
```
The registration of a user can be performed by any administrator on any client-application offering a suitable GUI. Upon registration the password of the user is hashed and sent to the server while being encrypted within the session of the administrator to make sure no third party can listen and acquire the password. Then the server saves the `userId` and `password hash` in its credential store. Saving the `userId` and `password hash` on the client-application side is left to the application because saving techniques differ across different platforms (Android, Linux, iOS, etc).

#### Registration of a Client

The registration of a client has to be performed on the client that is to be registered. So the administrator has to be logged in on the machine running the client-application. This is because upon registration a `private/public key pair` is automatically generated by the client. The `private key` is saved in the client-application's credential store along the `clientId`. The `public key` is sent to the server which saves it in the its credential store along the `clientId`. The following code block should give a rough understanding of the procedure.
```
// --- on the client that is to be registered ---

// retrieve the SessionManager
SessionManager sessionManager = SessionManager.getInstance();

// login the administrator
sessionManager.login(adminId, adminPassword); // omit if already logged in

// register the client
sessionManager.registerClient(clientId);
```
Note that you do not provide a password for the client. This is because for communication between a client and the server private/public key pair encryption is used.

The following figure shows the creation a user or client:
![Sequence Diagram of User Registration](/img/bco/authentication/create_user.svg)

## User Authentication
:::note Note
In this section a user and a client are both referenced to as a user. When we write about the client in specific it will be made explicit.
:::

Authentication allows a user to access the BCO system with permissions explicitly specified for the given user. However, not every actions requires that a user is logged in. Some actions, as specified by the system administrators, can be executed with so called *other permissions*. Such actions can be performed by anyone and without being logged in.

### How to login a user

Logging in a user requires nothing more than calling the according method on an instance of the [SessionManager](#the-session-manager).
There are two different methods for logging in depending on if you want to login as a user or as a client.
```
// retrieve the SessionManager
SessionManager sessionManager = SessionManager.getInstance();

// login a user
sessionManager.login(userId, password, stayLoggedIn);

// or login a client
sessionManager.login(clientId);
```
Where `stayLoggedIn` is a boolean that tells the system if the user wants to stay logged in during the current session. More on that later.

There is nothing else the application on the client machine has to do after the login. The user can now perform actions for which he has permissions.

:::note Note
Since a client and user can be logged in from the same application it is not necessary to log out a client prior to logging in a user.
:::

Following diagram illustrates what happens during the login process in the background.
![](/img/bco/authentication/login.svg)

Following diagram illustrates the login process and a subsequent request to performing an action on the system.
![](/img/bco/authentication/authentication.svg)

Following diagram illustrates the Kerberos algorithm that is used to authenticate / login a user.
![](/img/bco/authentication/kerberos.svg)

### How to keep a user logged in over long time

Keeping a user logged in over time is preferential but gives rise to a security issue. An *open connection* could be misused by a third party to perform unauthorized actions on the system. However, it provides the comfort of not having to login again and again after longer period of inactivity. Therefore the implementation allows a user or client to login again automatically once their session expired and thus to stay logged in.

How does a session expire? The Kerberos authentication algorithm is not discussed in detail here. For a more detailed explanation checkout the [protocol definition](https://en.wikipedia.org/wiki/Kerberos_(protocol)). During authorization every user gets a ticket that he has to present to the server on every action he wants to perform. This is similar to an Id-card in real life that is shown to authorities when they ask you to verify yourself. This ticket, however, has a specified period in which it is valid. Once this period expires the ticket cannot be used anymore and the user has to login again. But this validity period is updated on every action by the server. So as long as the user keeps performing actions in the specified period his session does not expire. This means that when a user does not perform an action for a longer period of time his session expires.

#### Keep a user logged in

Keeping a user logged in is in general more risky than keeping a client logged in. Often a user has more sensible permissions on the system than a client. Due to this circumstance we implemented the option to keep the user logged in by his own choice during the login process. This option is set with the `stayLoggedIn` flag in `SessionManager.login(userId, password, stayLoggedIn)`. If `stayLoggedIn` is specified as `true`, the `userId` and `password` will be saved by the Session Manager but they stay inaccessible from the outside of the class. Should the current session expire the user will be automatically logged in again.
:::note Note
Once the application is restarted the session variables will be reset as well and the user will most definitely have to login manually again.
:::

#### Keep a client logged in

There's nothing for the client application to do to keep a client logged in. This happens automatically by default.

Following diagram illustrates the relogging process.
![](/img/bco/authentication/action.svg)

## User Authorization
This section describes what different permissions are for, whose permissions are checked and how permissions for units can be evaluated indirectly.

### Permission Types and Their Meaning

This section describes the differences of read, write, and access permissions and their implications on the user's or client's actions. The implications of the permissions are oriented towards and thus very similar to those of the standard Unix permissions. 

**Location Permissions**

|   | Unix Directories | BCO Locations  |
| - |:------------:| :-----:|
| R | read content | read internal unit configs |
| W | write        | change configuration of the location |
| X | access       | change status/apply actions & see internal unit ids |

**Unit Permissions**

|   | Unix Files | BCO Units                        |
| - |:-------:| :-------------------------: |
| R | read    | read status                 |
| W | write   | change configuration of the unit       |
| X | execute | change status/apply actions |

#### Read

Read permission allows the user or client to see the status of a unit e.g. if a light is turned on. If a user has write or access permission he should also have read permission.
Everyone always has read permissions on the root location.

#### Write

Write permission grants a user write access on a given unit. This means the user is allowed to change the configuration of a unit like its label or accessibility. Having write permission does not imply automatic access permission.

#### Access

Access permission grants the user the ability to use units. For example, with access permission for a light bulb a user can switch it on and off. Having access permission does not imply automatic write permission.

### Whose Permissions Are Checked

There are four different cases:

1. The application is not authenticated as a client and no user is logged in
2. The application is not authenticated as a client and a user is logged in
3. The application is authenticated as a client and no user is logged in
4. The application is authenticated as a client and a user is logged in

In each of these states the system has to determine which permissions need to be checked when the application sends a request to perform an action. 

In the first case *other* permissions are assumed. This is because neither a user nor a client are logged in so there is none to refer to. *Other* permissions are specifically set for this case.

Obviously, in the second and third case the permissions of the client or the user are assumed.

In the fourth case if both a client and a user are logged in, both their permissions are checked. This means that its enough if either the user or the client has permissions to perform the action. Only if neither has necessary permissions the execution of the action is refused.

### Indirect Permissions

If a system provides a lot of units it can be a tedious task to set the permissions for every unit individually.
Therefore it is possible to leave the permission configuration of a unit empty. Whenever an action should be performed and the according unit does not have a permission configuration the permission configuration is evaluated indirectly. This is done by using the location hierarchy of the BCO system. If the unit does not have a permission configuration the permission configuration of the location where the unit is placed is used. If again the location does not provide a permission configuration its parent location is checked. Thus for a working system only the root location needs to have a permission configuration. Furthermore this makes it easy to define permissions for a whole room.

## Accessing User Settings
This section lists methods that return or change user account specific settings. In this case user account specific settings mean relevant information for the authorization process which are the password and the admin flag of a user.

For all methods listed below it is always necessary that a user or client is logged in. Furthermore some methods require the currently logged in user to be an administrator. Therefore in the beginning of each section it is mentioned if it is necessary to be logged in as an administrator.

Additionally all methods listed are always performed on an instance of the `SessionManager` on the client side. For obtaining an instance remember:

    SessionManager sessionManager = SessionManager.getInstance();

### Check if a user has administrator permissions

> A user has to be logged in
```
boolean isAdmin = sessionManager.isAdmin()
```

This method checks if the currently logged in user is an administrator or not. Note that you cannot check if a client is an administrator. Clients can never be administrators for security reasons.

### Change a user's password

> A user or an administrator has to be logged in
```
sessionManager.changeCredentials(String userId, String oldPassword, String newPassword)
```
This method allows a logged in user to change his own password. If you set `id = null` the id will be set automatically to the currently logged in user.
As an administrator you can also change the password of other users. For that, simply plug in the id of the user whose password should be changed.  

### Appoint a user an admin (or the opposite way)

> An administrator has to be logged in
```
sessionManager.setAdministrator(String userId, boolean isAdmin)
```
Appoints a regular user with `userId` an administrator or revokes the status. That depends on the boolean `isAdmin`. Note that an administrator cannot revoke his own rights. This ensures that there is always at least one administrator active in the whole system.

### How does all of this work in detail? 

The following diagram shows how changing the password of a user works in detail. This also illustrates the background processes of all the other methods.

1. The `SessionManager` checks if the entity performing the request is logged in and forwards the request to the `AuthenticationController`
2. The `AuthenticationController` verifies that the entity performing the request has the necessary permissions
3. (In this case it also checks if the old password was correct)
4. Then the `AuthenticationController` updates its internal representations of the user in its own credential store
5. In the end it returns an updated ticket that is necessary for keeping the current session active

![](/img/bco/authentication/change_credentials.svg)

## Development of Authenticated Services
This section describes how the authentication module can be integrated into a server side application that provides services which need authentication and authorization to be executed.

There are three components designed to support this:

* AuthenticatedServerManager
* AuthorizationHelper
* AuthenticatedServiceProcessor

### Authenticated Server Manager

Every time a user wants to execute an authenticated service he has to send his ticket with his request. The server that provides this service needs to be able to evaluate this ticket. The `AuthenticatedServerManager` is there to help with this. It logs in with a special account at the AuthenticatorController which is allowed to request a key that is needed to evaluate a ticket. This is automatically done when you create an instance. Important to note is that the AuthenticatorController creates this account on its first start and will save the private key needed to login with this account in a credentials folder only accessible by the user who started it. As long as the server application you are running has access to this file it will be able to login with the special account.
After creation you can call a method which evaluates the ticket as seen below. It will throw an exception when the evaluation fails and else returns a wrapper containing the id of the user who requested the execution and an updated ticket which the server needs to return in a response.
```
// evaluate a ticket send with a request
TicketEvaluationWrapper wrapper = AuthenticatedServerManager.getInstance().evaluateClientServerTicket(ticket)
// get the ticket which has to be returned to the user after execution of the service
wrapper.getTicketAuthenticatorWrapper()
// get the id of the user who requested the execution of the service
wrapper.getUserId()
```
### Authorization Helper

This is everything that needs to be done for the authentication process on the server side. To use authorization as well you can use the `AuthorizationHelper`. This class provides methods to test if a user has specific permissions for a unit configuration. An example for access permissions could look like this:
```
// test if a user with userId has access permissions on the unit with unitConfig
canAccess = AuthorizationHelper.canAccess(unitConfig, userId, groups, locations)
```
The locations and groups need to be retrieved from the registry like this:
```
locations = Registries.getUnitRegistry().getLocationUnitConfigRemoteRegistry().getEntryMap()
groups    = Registries.getUnitRegistry().getAuthorizationGroupUnitConfigRemoteRegistry().getEntryMap()
```
### Authenticated Service Processor

Another helper class which can do both authorization and authentication for server as well as remote side is the `AuthenticatedServiceProcessor`. However, this is only possible if the service is configured to receive an AuthenticatedValue as its sole parameter. The authenticated value contains the ticket of a user and an additional value encrypted with the session key of the user. The AuthenticatedValueProcessor automatically decrypts the additional value into a configured class which you can perform any action with. For a more specific explanation it is useful to have a look at the [JavaDoc](https://openbase.github.io/bco.authentication/apidocs/).