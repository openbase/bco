---
title: openbase - Projects
permalink: "/openbase-project-overview/"
layout: default
---

# Openbase Projects

<table style="width: 110%; border: 0px; margin: 0px auto;">
    <tr>
        <th></th>    
	    <th>Source</th>
        <th>Version</th>
        <th>Master Build</th>
        <th>LatestStable Build</th>
        <th>CodeFactor</th>
        <th>API Dosc</th>
    </tr>
    {% for project in site.data.projects %}
        <tr>
            <td>
                <b>{{ project.label }}</b><br>
            </td>    
	        <td align="center">
                <a href="https://github.com/openbase/{{ project.id }}" style="color: inherit;">
                    <i class="fa fa-github fa-lg"></i>
                </a>
            </td>
            <td align="center" valign="middle">
                <a href="https://maven-badges.herokuapp.com/maven-central/{{ project.group }}/{{ project.artifact }}">
                    <img style="width:90px;height:20px;" alt="Maven Central" src="http://img.shields.io/maven-central/v/{{ project.group }}/{{ project.artifact }}.svg?style=flat"/>
                </a>
            </td>
            <td align="center" valign="middle">
                <a href="https://travis-ci.org/openbase/{{ project.id }}">
                    <img style="width:90px;height:20px;" src="https://travis-ci.org/openbase/{{ project.id }}.svg?branch=master" alt="Master"/>
                </a>
            </td>
            <td align="center" valign="middle">
                <a href="https://travis-ci.org/openbase/{{ project.id }}">
                    <img style="width:90px;height:20px;" src="https://travis-ci.org/openbase/{{ project.id }}.svg?branch=latest-stable" alt="LatestStable"/>
                </a>
            </td>
            <td>
                <a href="https://www.codefactor.io/repository/github/openbase/{{ project.id }}/overview/master">
                    <img style="width:90px;height:20px;" src="https://www.codefactor.io/repository/github/openbase/{{ project.id }}/badge/master" alt="Codefactor"/>
                </a>
            </td>
            <td align="center" valign="middle">
                <a href="https://openbase.github.io/{{ project.id }}/apidocs" style="color: inherit;">
                    <i class="fa fa-book fa-lg"></i>
                </a>
            </td>
        </tr>
    {% endfor %}
</table>

	
