import os
import re

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    modified = False

    # Replace Icons.Filled.X with Icons.Outlined.X
    if "Icons.Filled." in content:
        content = content.replace("Icons.Filled.", "Icons.Outlined.")
        modified = True
        
    # Replace Icons.Default.X with Icons.Outlined.X
    if "Icons.Default." in content:
        content = content.replace("Icons.Default.", "Icons.Outlined.")
        modified = True

    if modified:
        with open(filepath, 'w') as f:
            f.write(content)

for root, _, files in os.walk("app/src/main/java/com/example/ui"):
    for file in files:
        if file.endswith(".kt"):
            process_file(os.path.join(root, file))
