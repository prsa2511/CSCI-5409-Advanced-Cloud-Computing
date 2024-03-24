provider "google" {
  project = "advanced-cloud"
  region  = "us-central1"
  credentials = file("./advanced-cloud-652a103aac8f.json")
}
 
 
resource "google_container_cluster" "k8s-cluster" {
  name               = "k8s"
  location           = "us-central1-a"
  initial_node_count = 1
 
 
  node_config {
    machine_type = "e2-medium"
    disk_size_gb = 10
    disk_type = "pd-standard"
    image_type = "COS_CONTAINERD"
  }
}
 
 
resource "google_compute_disk" "persistent_disk" {
  name  = "k8s-disk"
  type  = "pd-standard"
  zone  = "us-central1-a"
  size = 10
}