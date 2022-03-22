#!/bin/bash

#!/bin/bash

# inform user about actions
echo "WARNING: If you continue, all project changes will be erased!!!"

# await confirmation
read -p "         Please type y to confirm: " -n 1 -r &&
if [[ ! $REPLY =~ ^[YyZz]$ ]]; then
    echo -e "\r         Reset aborted by user.        "
    exit 255
fi
echo # new line

# reset top level project
git reset --hard
git clean -f

# make sure all submodules are initialized and updated.
./init_update.sh

# reset submodule projects
git submodule foreach git reset --hard
git submodule foreach git clean -f
