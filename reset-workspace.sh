#!/bin/bash

#!/bin/bash

# inform user about actions
echo "WARNING: If you continue, all project changes will be erased!!!"

# await confirmation
read -p "Please type y to confirm: " -n 1 -r &&
echo &&  # new line
if [[ ! $REPLY =~ ^[YyZz]$ ]]; then
    echo "=== Reset aborted by user..."
    return 255
fi

# reset top level project
git reset --hard
git clean -f

# make sure all submodules are initialized and updated.
./init_update.sh

# reset submodule projects
git submodule foreach git reset --hard
git submodule foreach git clean -f
