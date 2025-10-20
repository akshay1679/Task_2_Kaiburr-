# Task 2 Kaiburr

This project is a Spring Boot application that provides a REST API for managing and executing tasks. The tasks are stored in a MongoDB database, and the commands are executed inside temporary Kubernetes pods.

-----

## Features

  * **REST API:** for creating, retrieving, updating, and deleting tasks.
  * **Task Execution:** an endpoint to execute the command associated with a task.
  * **Kubernetes Integration:** uses the Fabric8 Kubernetes client to create and manage pods for command execution.
  * **MongoDB Integration:** uses Spring Data MongoDB to store and retrieve task information.
  * **Containerized:** includes a Dockerfile for easy containerization and deployment.
  * **Kubernetes Ready:** includes Kubernetes manifest files for deployment.

-----

## Technologies Used

  * **Java 17**
  * **Spring Boot**
  * **Spring Data MongoDB**
  * **Maven**
  * **Docker**
  * **Kubernetes**
  * **Fabric8 Kubernetes Client**

-----

## Prerequisites

  * **Java 17** or higher
  * **Maven**
  * **Docker**
  * **Minikube** or any other Kubernetes cluster
  * **kubectl**

-----

## Getting Started

### 1\. Clone the repository

```bash
git clone https://github.com/akshay1679/Task_2_Kaiburr-.git
cd Task_2_Kaiburr-
```

### 2\. Build the application

You can build the application using the Maven wrapper included in the project.

```bash
./mvnw clean package
```

### 3\. Build the Docker image

```bash
docker build -t akshay1679/kaiburr-task-app:latest .
```

### 4\. Load the Docker image into Minikube

If you are using Minikube, you need to load the Docker image into the Minikube's Docker daemon.

```bash
minikube image load akshay1679/kaiburr-task-app:latest
```

### 5\. Deploy to Kubernetes

The `k8s` directory contains the Kubernetes manifest files for deploying the application and MongoDB.

**a. Create the MongoDB secret:**

You need to create a secret to store the MongoDB connection URI. The `mongo-secret.yaml` file contains a base64 encoded URI. You can apply it directly.

```bash
kubectl apply -f k8s/mongo-secret.yaml
```

**b. Deploy MongoDB:**

This project assumes a MongoDB instance is running in the cluster. You can deploy one using a Helm chart or a simple deployment file. For this project, we'll assume a service named `mongo-mongodb` is available.

**c. Create the Role and RoleBinding for the application:**

The application needs permissions to create, get, watch, and delete pods. The `rbac.yaml` file defines the necessary roles and bindings.

```bash
kubectl apply -f k8s/rbac.yaml
```

**d. Deploy the application:**

The `app-deployment.yaml` file defines the deployment for the Spring Boot application. Make sure the `image` field in `app-deployment.yaml` matches the Docker image you built.

```yaml
# k8s/app-deployment.yaml
# ...
      containers:
      - name: kaiburr-app
        # IMPORTANT: Change this to your Docker Hub username and image name
        image: akshay1679/kaiburr-task-app:latest
        imagePullPolicy: IfNotPresent # Use this if you loaded the image locally to minikube
# ...
```

Now, apply the deployment and the service.

```bash
kubectl apply -f k8s/app-deployment.yaml
kubectl apply -f k8s/app-service.yaml
```

### 6\. Access the application

You can access the application by port-forwarding the service.

```bash
kubectl port-forward service/kaiburr-app-service 8080:8080
```

The application will be available at `http://localhost:8080`.

-----

## API Endpoints

The following are the available API endpoints:

| Method | Endpoint              | Description                                   |
| ------ | --------------------- | --------------------------------------------- |
| `GET`    | `/tasks`              | Get all tasks.                                |
| `GET`    | `/tasks/{id}`         | Get a task by its ID.                         |
| `GET`    | `/tasks/search?name={name}` | Search for tasks by name.              |
| `PUT`    | `/tasks`              | Create or update a task.                      |
| `DELETE` | `/tasks/{id}`         | Delete a task by its ID.                      |
| `PUT`    | `/tasks/{id}/execute` | Execute the command associated with a task.   |

### Example `Task` object:

```json
{
  "name": "List Files",
  "owner": "Akshay",
  "command": "ls -l"
}
```

-----
