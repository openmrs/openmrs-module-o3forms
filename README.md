# O3

## Description

This module provides backend services for the O3 system. This mainly consists of REST APIs that don't have another home
useful or necessary for parts of O3's operations.

Currently, this module provides:

* A REST API to get a "complete" O3 form schema

## Features

### O3 Form Schema

This feature allows the frontend to request an O3 form and receive a form object that has:

* All referenced forms loaded into the appropriate parts of the main form
* All translations provided in a translation file for the appropriate locale
* Metadata about concepts used in the forms

The goal of this feature is to reduce the overall traffic between a browser and the OpenMRS server involved in loading a form.
m
