---
title: openbase - Contact
permalink: "/contact/"
layout: default
---

# Developer Contact

## Slack

Feel free to join!

* Team: openbase-org
* Channel: https://openbase-org.slack.com/#support

## Mail Support

support@openbase.org

## Issue Reports

Please report issues via github!

* https://github.com/openbase/
{% for project in site.data.projects %}
  * {{ project.label }}
    * https://github.com/openbase/{{ project.id }}/issues/new
{% endfor %}
