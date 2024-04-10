import os
import secrets
import json
import base64
import boto3
from werkzeug.utils import secure_filename
from flask import Flask, request, render_template, redirect, url_for, flash, session
from werkzeug.security import generate_password_hash, check_password_hash
from flask_sqlalchemy import SQLAlchemy

# Generate a random secret key
#secret_key = secrets.token_hex(16)

# Initialize boto3 clients
lambda_client = boto3.client('lambda', region_name='us-east-1')
sns_client = boto3.client('sns', region_name='us-east-1')

def ensure_topic_exists(client, topic_name):
    response = client.list_topics()
    topic_arn = next((topic['TopicArn'] for topic in response['Topics'] if topic['TopicArn'].endswith(':' + topic_name)), None)
    
    if not topic_arn:
        create_topic_response = client.create_topic(Name=topic_name)
        topic_arn = create_topic_response['TopicArn']
        print(f"Created new topic: {topic_arn}")
    else:
        print(f"Topic already exists: {topic_arn}")
    return topic_arn

topic_arn = ensure_topic_exists(sns_client, 'JournalNotifications')
email_address = 'pr711119@dal.ca'
response = sns_client.subscribe(TopicArn=topic_arn, Protocol='email', Endpoint=email_address)

def process_image_with_lambda(image_file):
    try:
        image_data = image_file.read()
        encoded_string = base64.b64encode(image_data).decode("utf-8")
        payload = {"image": encoded_string}
        response = lambda_client.invoke(FunctionName='ProcessImage-CloudF', InvocationType='RequestResponse', Payload=json.dumps(payload))
        response_payload = json.loads(response['Payload'].read().decode("utf-8"))
        body = json.loads(response_payload['body'])
        processed_image_base64 = body['image']
        processed_image_data = base64.b64decode(processed_image_base64)
        return processed_image_data
    except Exception as e:
        print(f"An error occurred: {str(e)}")
        return None

db = SQLAlchemy()

class Entry(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(100), nullable=False)
    content = db.Column(db.Text, nullable=False)
    date = db.Column(db.String(10), nullable=False)
    image_filename = db.Column(db.String(200), nullable=True)

    def __repr__(self):
        return f'<Entry {self.title}>'
        
class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    password_hash = db.Column(db.String(128))

    def __repr__(self):
        return f'<User {self.username}>'

def create_app():
    app = Flask(__name__)
    secret = get_secret()
    credentials = json.loads(secret)
    DATABASE_URI = f"mysql+pymysql://{credentials['username']}:{credentials['password']}@{credentials['host']}:{credentials['port']}/{credentials['dbClusterIdentifier']}"

    app.config['SQLALCHEMY_DATABASE_URI'] = DATABASE_URI
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    app.config['SQLALCHEMY_ECHO'] = True
    app.config['UPLOAD_FOLDER'] = 'static/uploads'
    app.config['ALLOWED_EXTENSIONS'] = {'png', 'jpg', 'jpeg', 'gif'}
    app.config['SECRET_KEY'] = 'e387f997167d4ef32ba1d65b95bc5c70'
    
    db.init_app(app)
    
    with app.app_context():
        db.create_all()

    @app.route('/')
    def home():
        return redirect(url_for('welcome'))
	
    @app.route('/index')
    def index():
        entries = Entry.query.all()
        return render_template('index.html', entries=entries)
    
    @app.route('/logout')
    def logout():
        session.pop('user', None)
        return redirect(url_for('welcome'))

    @app.route('/welcome')
    def welcome():
        return render_template('welcome.html')

    @app.route('/add', methods=['GET', 'POST'])
    def add_entry():
        if request.method == 'POST':
            title = request.form['title']
            content = request.form['content']
            date = request.form['date']
            image_filename = None

            if 'image' in request.files:
                file = request.files['image']
                if file.filename != '':
                    filename = secure_filename(file.filename)
                    processed_image_data = process_image_with_lambda(file)
                    if processed_image_data:
                        processed_filename = f"processed_{filename}"
                        file_path = os.path.join(app.config['UPLOAD_FOLDER'], processed_filename)
                        with open(file_path, "wb") as processed_file:
                            processed_file.write(processed_image_data)
                        image_filename = processed_filename

            new_entry = Entry(title=title, content=content, date=date, image_filename=image_filename)
            db.session.add(new_entry)
            db.session.commit()
            return redirect(url_for('index'))
        return render_template('add_entry.html')

    @app.route('/edit/<int:entry_id>', methods=['GET', 'POST'])
    def edit_entry(entry_id):
        entry = Entry.query.get_or_404(entry_id)
        if request.method == 'POST':
            entry.title = request.form['title']
            entry.content = request.form['content']
            entry.date = request.form['date']
            
            if 'image' in request.files:
                file = request.files['image']
                if file and file.filename != '':
                    if entry.image_filename:  # If an old image exists, remove it
                        os.remove(os.path.join(app.config['UPLOAD_FOLDER'], entry.image_filename))
                    filename = secure_filename(file.filename)
                    processed_image_data = process_image_with_lambda(file)
                    processed_filename = f"processed_{filename}"
                    file_path = os.path.join(app.config['UPLOAD_FOLDER'], processed_filename)
                    with open(file_path, "wb") as processed_file:
                        processed_file.write(processed_image_data)
                    entry.image_filename = processed_filename

            db.session.commit()
            return redirect(url_for('index'))
        return render_template('edit_entry.html', entry=entry)

    @app.route('/delete/<int:entry_id>')
    def delete_entry(entry_id):
        entry = Entry.query.get_or_404(entry_id)
        if entry.image_filename:
            os.remove(os.path.join(app.config['UPLOAD_FOLDER'], entry.image_filename))
        db.session.delete(entry)
        db.session.commit()
        return redirect(url_for('index'))

    @app.route('/register', methods=['GET', 'POST'])
    def register():
        if request.method == 'POST':
            username = request.form['username']
            email = request.form['email']
            password = request.form['password']
            user = User(username=username, email=email, password_hash=password)
            db.session.add(user)
            db.session.commit()
            
            # Send SNS notification
            sns_client.publish(
                TopicArn=topic_arn,
                Message=f'Happy Journaling {username}',
                Subject='Welcome to Journal!'
            )
            
            flash('Registration successful.')
            return redirect(url_for('welcome'))
        return render_template('register.html')

    @app.route('/login', methods=['GET', 'POST'])
    def login():
        if request.method == 'POST':
            username = request.form['username']
            password = request.form['password']
            user = User.query.filter_by(username=username).first()
            print(f"is the info being fetched or no?")		
            
            if user and user.password_hash == password:
                # Log the user in
                session['user'] = user.id
                flash('Login successful.')
                print(f"Check if they are reaching inside the success route")
                return redirect(url_for('index'))
            else:
                flash('Invalid username or password.')
                print(f"Check if they are reaching inside the failure route")
        return render_template('login.html')
        
    return app

def get_secret():
    secret_name = "db_secret-CloudFormation1"
    region_name = "us-east-1"

    # Create a Secrets Manager client
    session = boto3.session.Session()
    client = session.client(service_name='secretsmanager', region_name=region_name)

    try:
        get_secret_value_response = client.get_secret_value(SecretId=secret_name)
    except Exception as e:
        raise e

    secret = get_secret_value_response['SecretString']
    return secret
	
app = create_app()

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
