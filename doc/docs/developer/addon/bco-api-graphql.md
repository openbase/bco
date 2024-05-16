# BCO GraphQL API

[Source Code](https://github.com/openbase/bco.api.graphql)

## Bonjour Service Advertising

Service Name ```bco-api-graphql```

## Default Settings
* Suffix: ```graphql```
* Port: ```13781```
* Example Endpoint: ```http://localhost:13781/graphql```

## Supported Queries
* `login(username: String!, passwordHash: String!): String` - Retrieve an authentication token (see [Authorization Header](#supported-headers))
  * Default admin base64 password hash: `R+gZ+PFuauhav8rRVa3XlWXXSEyi5BcdrbeXLEY3tDQ=`
  * Checkout [how to generate the password hash on client side](#how-to-generate-the-password-hash-on-client-side) for more details. 
* `verifyToken(token: String): Boolean`

## Supported Mutations
* `changePassword(username: String!, oldPassword: String!, newPassword: String!): Boolean` - Note: the return value will always be true since an exception is thrown if changing the password fails.
* `updateLabel(unitId: String, label: String): Label` - Update the label of a unit. Note: this will update the first label considering the current language code which can be provided with the [Accept-Language Header](#supported-headers).
* `updateLocation(unitId: String, locationId: String): PlacementConfig` - Update the location of a unit.
* `updateFloorPlan(locationId: String, shape: Shape): Shape` - Clear the floor list of the current location and replace it with the list in the provided shape. Note: the rejoiner framework does not allow passing lists as arguments so you have to provide a complete shape from which only the floor list is considered.
* `updatePose(unitId: String, pose: Pose): Pose` - Replace the pose of a unit.
* `registerUnitConfig(unitConfig: UnitConfig): UnitConfig` - Register a new unit.
* `removeUnitConfig(unitId: String): UnitConfig` - Remove a unit by its id.
* `updateUnitConfig(unitConfig: UnitConfig): UnitConfig` - Update a unit config. Note: the values of the provided unit config will be merged into the current one (retrieved through the provided id). This means that all values which are lists will just be appended to the current list.
* `updateMetaConfig(unitId: String, entry: Entry): MetaConfig` - Update an entry in the toplevel meta config of a unit config.

## Supported Headers
The GraphQL API Server will process the following HTTP headers on requests:

* **Authorization Header**:
The `authorization` header can be used to authenticate your client at BCO.
Just send the token received through a [login query](#supported-queries) as the value of the authorization header with your requests. 
If this header is not supplied, your actions will be performed with other permissions.

* **Accept-Language Header**:
If you set the `accept-language` header to a language code, the GraphQL API will resolve all multi language strings (eg. labels and descriptions) according to this language.
On queries the text matching the language code are returned and on mutations the values matching your language code are modified.
It this header is not set, the default of the server running the GraphQL API is used.

## How to generate the password hash on client side

The `passwordHash` needs to be generated as follows:
1. Encoding the plain text password as `UTF16`
2. Compute a hash of the `UTF16` bytes by using the `SHA-256` hash generator algorithm
3. Encode the result as `Base64` and pass it to the login method as `passwordHash`.

Dart Example:
   ```dart
   final hashedPassword = base64.encode(
      sha256.convert(
         encodeUtf16(plainPassword)
      ).bytes);
   ```
Java Example:
   ```java
   final String hashedPassword = Base64.getEncoder()
      .encodeToString(
         EncryptionHelper.hash("plainPassword")
      );
   ```
## Used Tools

1. [GraphQL](https://graphql.org/)
   * API Framework
2. [Rejoiner](https://rejoiner.io/)
   * Protobuffer type integration
4. [Bonjour Service](https://github.com/jmdns/jmdns)
   * Advertising and discovery of the graphql service within the local network
