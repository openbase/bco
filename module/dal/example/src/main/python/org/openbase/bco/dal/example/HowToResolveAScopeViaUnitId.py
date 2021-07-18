#!/usr/bin/env python2

import rsb

if __name__ == '__main__':
    with rsb.createRemoteServer("/registry/unit/ctrl") as unit_registry:
        print unit_registry.getUnitScopeById("07b909bb-c331-4dec-8a92-4fdb036316c9")
