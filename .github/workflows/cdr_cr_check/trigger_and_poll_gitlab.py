import datetime
import os
import sys
from time import sleep
import requests
import logging

complete_statuses = ["failed", "success", "canceled"]
trigger_token = os.getenv("TRIGGER_TOKEN")
project_api_read_token = os.getenv("READ_API_TOKEN")
current_clinical_reasoning_branch = os.getenv("CLINICAL_REASONING_BRANCH")
target_cdr_cr_branch = os.getenv("CDR_CR_BRANCH")

if not trigger_token:
    print("CDR-CR failed to run because TRIGGER_TOKEN was not set")
    sys.exit(1)

if not project_api_read_token :
    print("CDR-CR failed to run because READ_API_TOKEN was not set or has insufficient permissions")
    sys.exit(1)

# Enable logging
logging.basicConfig(level=logging.DEBUG)
logging.getLogger("urllib3").setLevel(logging.DEBUG)

if not target_cdr_cr_branch:
    print("Defaulting CDR-CR branch to main as this is an automatic build.")
    target_cdr_cr_branch = "main"

form_data = {
    "token": trigger_token,
    "ref": target_cdr_cr_branch,
    "variables[CLINICAL_REASONING_BRANCH]": current_clinical_reasoning_branch
}
print(f"About to start job. [target_cdr_cr_branch={target_cdr_cr_branch}, current_clinical_reasoning_branch ={current_clinical_reasoning_branch}]")
print("Triggering Remote CI process on gitlab.com.")

result = requests.post("https://gitlab.com/api/v4/projects/66223517/trigger/pipeline", data=form_data)
if result.status_code > 399:
    print(f"status code: {result.status_code}, result.json(): {result.json()}, headers: {result.headers}")

trigger_json = result.json()
pipeline_id = trigger_json["id"]

def poll_for_pipeline_status(pipeline_id):
    query_params = {
        "private_token": project_api_read_token
    }
    resp = requests.get(f"https://gitlab.com/api/v4/projects/66223517/pipelines/{pipeline_id}", params=query_params)
    pipeline_status_json = resp.json()
    return pipeline_status_json


print(f"Generated pipeline. [pipeline_id={pipeline_id}]")
status = None
status_json = poll_for_pipeline_status(pipeline_id)
start_time = datetime.datetime.now()

while status not in complete_statuses:
    status = status_json["status"]
    now = datetime.datetime.now()
    print(f"Job not yet complete. [status={status}, duration={(now - start_time).total_seconds()}s]")
    sleep(10)
    status_json = poll_for_pipeline_status(pipeline_id)

if status == "success":
    print(f"CDR-CR compiled against this branch! Please visit: {status_json['web_url']}")
    sys.exit(0)
else:
    print(f"CDR-CR failed against this branch! Please visit: {status_json['web_url']}")
    sys.exit(1)