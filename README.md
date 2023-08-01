# O3 Forms

## Description

This module provides REST APIs to provide backend services for O3-style forms

## Requirements

This module requires OpenMRS 2.6.0 or higher and the webservices.rest module 2.40.0 or higher.

## REST APIS

### O3 Form Schema

This feature allows the frontend to request an O3 form and receive a form object that has:

* All referenced forms loaded into the appropriate parts of the main form
* All translations provided in a translation file for the appropriate locale
* Metadata about concepts used in the forms

The goal of this feature is to reduce the overall traffic between a browser and the OpenMRS server involved in loading a form.

#### Retrieve a complete form

```http request
GET /ws/rest/v1/o3/forms/<NAME_OR_UUID>
```

| URL Parameter  | Type     | Description                                         |
|:---------------|:---------|:----------------------------------------------------|
| `NAME_OR_UUID` | `string` | Either the name or UUID of the form to be displayed |

| Query Parameter            | Type      | Description                                                                              |
|:---------------------------|:----------|:-----------------------------------------------------------------------------------------|
| `includeConceptReferences` | `boolean` | Whether or not to include the conceptReferences object. Defaults to true.                |
| `v`                        | `string`  | REST API-style formatting for the conceptReferences. Defaults to `custom:(uuid,display)` |

If the form exists and everything process correctly, you should get a response like:
```http response
HTTP/1.1 200 OK
Cache-Control: no-cache; private
Content-Type: application/json;charset=UTF-8
Etag: "<some value>"

<JSON Content>
```

Other responses will be returned as appropriate. The API will return a 404 with an explanation if the form cannot be found
or the referenced form does not have the appropriate form resources associated with it.

If an error occurs either generating the translations section or the concept references (if requested), those elements will
simply not be present and appropriate messages will log to the backend. This is to ensure that we don't have form failures
just due to issues with translations or concepts. It is assumed that the consuming application will take appropriate action
if these elements do not exist.
