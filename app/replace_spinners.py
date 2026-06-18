import os
import re

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Add import
    if "import com.example.ui.theme.*" not in content and "IronLogApp" not in filepath and "LoginScreen" not in filepath and "Theme" not in filepath:
        content = content.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport com.example.ui.theme.*")

    # Replace specific CircularProgressIndicators
    content = re.sub(
        r'CircularProgressIndicator\(color = [^\)]+\)',
        r'Box(modifier = Modifier.fillMaxWidth().height(200.dp).skeleton().clip(RoundedCornerShape(IronSpacing.CardCornerRadius)))',
        content
    )
    
    with open(filepath, 'w') as f:
        f.write(content)

for root, _, files in os.walk("app/src/main/java/com/example/ui"):
    for file in files:
        if file.endswith(".kt") and "DesignSystem" not in file:
            process_file(os.path.join(root, file))
