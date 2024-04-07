import os
from werkzeug.utils import secure_filename
from flask import Flask, request, render_template, redirect, url_for
from flask_sqlalchemy import SQLAlchemy
import boto3
import json

# Define the database model
db = SQLAlchemy()

class Entry(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(100), nullable=False)
    content = db.Column(db.Text, nullable=False)
    date = db.Column(db.String(10), nullable=False)
    image_filename = db.Column(db.String(200), nullable=True)

    def __repr__(self):
        return '<Entry %r>' % self.title

def create_app():
    app = Flask(__name__)
    
    # Load the database configuration from AWS Secrets Manager or other source
    secret = get_secret()
    credentials = json.loads(secret)
    DATABASE_URI = f"mysql+pymysql://{credentials['username']}:{credentials['password']}@{credentials['host']}:{credentials['port']}/{credentials['dbClusterIdentifier']}"

    app.config['SQLALCHEMY_DATABASE_URI'] = DATABASE_URI
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    app.config['SQLALCHEMY_ECHO'] = True
    app.config['UPLOAD_FOLDER'] = 'static/uploads'
    app.config['ALLOWED_EXTENSIONS'] = {'png', 'jpg', 'jpeg', 'gif'}
    
    db.init_app(app)
    
    with app.app_context():
        db.create_all()

    # Define routes
    @app.route('/')
    def index():
        entries = Entry.query.all()
        return render_template('index.html', entries=entries)

    def allowed_file(filename):
        return '.' in filename and \
               filename.rsplit('.', 1)[1].lower() in app.config['ALLOWED_EXTENSIONS']

    @app.route('/add', methods=['GET', 'POST'])
    def add_entry():
        if request.method == 'POST':
            title = request.form['title']
            content = request.form['content']
            date = request.form['date']
            image_filename = None  # Default to no filename

            # Check if an image was uploaded
            if 'image' in request.files:
                file = request.files['image']
                if file and allowed_file(file.filename):
                    filename = secure_filename(file.filename)
                    file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
                    file.save(file_path)
                    image_filename = filename  # Update filename to the uploaded file's name

            # Now create the entry regardless of whether an image was uploaded
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
            
            # Handle new image upload
            if 'image' in request.files:
                file = request.files['image']
                if file and allowed_file(file.filename):
                    if entry.image_filename:  # If an old image exists, remove it
                        os.remove(os.path.join(app.config['UPLOAD_FOLDER'], entry.image_filename))
                    filename = secure_filename(file.filename)
                    file.save(os.path.join(app.config['UPLOAD_FOLDER'], filename))
                    entry.image_filename = filename  # Update the image filename in the database

            db.session.commit()
            return redirect(url_for('index'))
        return render_template('edit_entry.html', entry=entry)

    @app.route('/delete/<int:entry_id>')
    def delete_entry(entry_id):
        entry = Entry.query.get_or_404(entry_id)
        if entry.image_filename:
            os.remove(os.path.join(app.config['UPLOAD_FOLDER'], entry.image_filename))  # Delete the image file
        db.session.delete(entry)
        db.session.commit()
        return redirect(url_for('index'))

    return app

def get_secret():
    secret_name = "db_secret1"
    region_name = "us-east-1"

    # Create a Secrets Manager client
    session = boto3.session.Session()
    client = session.client(
        service_name='secretsmanager',
        region_name=region_name
    )

    try:
        get_secret_value_response = client.get_secret_value(
            SecretId=secret_name
        )
    except ClientError as e:
        raise e

    secret = get_secret_value_response['SecretString']
    return secret

app = create_app()

if __name__ == '__main__':
    app.run(debug=True)
