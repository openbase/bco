---
title: openbase - Contact
permalink: "/contact/"
layout: default
---

# Contact

## Developer Slack

Feel free to join!

* Team: [openbase-org](https://openbase-org.slack.com)
* Channel: [#support](https://openbase-org.slack.com/#support)

## Mail Support

support@openbase.org

## Issue Reports

Please report issues via: [github](https://github.com/openbase/)

{% for project in site.data.projects %}
* **{{ project.label }}**
  * [create new issue](https://github.com/openbase/{{ project.id }}/issues/new)
{% endfor %}
