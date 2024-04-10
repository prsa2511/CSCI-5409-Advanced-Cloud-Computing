import json
import base64
from PIL import Image
import io

def lambda_handler(event, context):
    try:
        # Decode the image data
        image_data = base64.b64decode(event['image'])
        image = Image.open(io.BytesIO(image_data))

        # Process the image (example: resize)
        # Note: Consider maintaining aspect ratio or validating dimensions
        image = image.resize((400, 260), Image.LANCZOS)

        # Save the image to a buffer
        buffer = io.BytesIO()
        image.save(buffer, format="JPEG", quality=85)
        image_bytes = buffer.getvalue()

        # Encode the processed image back to base64
        result_image_base64 = base64.b64encode(image_bytes).decode("utf-8")

        return {
            'statusCode': 200,
            'headers': {'Content-Type': 'application/json'},
            'body': json.dumps({
                'image': result_image_base64
            })
        }
    except Exception as e:
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }
