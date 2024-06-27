envValue=$1
APP_NAME=$2
OPENSHIFT_NAMESPACE=$3
COMMON_NAMESPACE=$4
APP_NAME_UPPER=${APP_NAME^^}
DB_JDBC_CONNECT_STRING=$5
DB_PWD=$6
DB_USER=$7
SPLUNK_TOKEN=$8
CHES_CLIENT_ID=$9
CHES_CLIENT_SECRET="${10}"
CHES_TOKEN_URL="${11}"
CHES_ENDPOINT_URL="${12}"
TZVALUE="America/Vancouver"
SOAM_KC_REALM_ID="master"
SOAM_KC=soam-$envValue.apps.silver.devops.gov.bc.ca

SOAM_KC_LOAD_USER_ADMIN=$(oc -n $COMMON_NAMESPACE-$envValue -o json get secret sso-admin-${envValue} | sed -n 's/.*"username": "\(.*\)"/\1/p' | base64 --decode)
SOAM_KC_LOAD_USER_PASS=$(oc -n $COMMON_NAMESPACE-$envValue -o json get secret sso-admin-${envValue} | sed -n 's/.*"password": "\(.*\)",/\1/p' | base64 --decode)
NATS_URL="nats://nats.${COMMON_NAMESPACE}-${envValue}.svc.cluster.local:4222"

echo Fetching SOAM token
TKN=$(curl -s \
  -d "client_id=admin-cli" \
  -d "username=$SOAM_KC_LOAD_USER_ADMIN" \
  -d "password=$SOAM_KC_LOAD_USER_PASS" \
  -d "grant_type=password" \
  "https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token" | jq -r '.access_token')

echo
echo Retrieving client ID for student-data-collection-api-service
SDC_APIServiceClientID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq '.[] | select(.clientId=="student-data-collection-api-service")' | jq -r '.id')

echo
echo Retrieving client secret for student-data-collection-api-service
SDC_APIServiceClientSecret=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$SDC_APIServiceClientID/client-secret" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq -r '.value')

echo
echo Removing STUDENT DATA COLLECTION API client if exists
curl -sX DELETE "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$SDC_APIServiceClientID" \
  -H "Authorization: Bearer $TKN"

echo
echo Writing scope READ_SDC_COLLECTION
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Read Student Data Collection Collection Data\",\"id\": \"READ_SDC_COLLECTION\",\"name\": \"READ_SDC_COLLECTION\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope READ_SDC_DISTRICT_COLLECTION
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Read Student Data Collection District Collection Data\",\"id\": \"READ_SDC_DISTRICT_COLLECTION\",\"name\": \"READ_SDC_DISTRICT_COLLECTION\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope WRITE_SDC_DISTRICT_COLLECTION
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Write Student Data Collection District Collection Data\",\"id\": \"WRITE_SDC_DISTRICT_COLLECTION\",\"name\": \"WRITE_SDC_DISTRICT_COLLECTION\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope DELETE_SDC_DISTRICT_COLLECTION
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Delete Student Data Collection District Collection Data\",\"id\": \"DELETE_SDC_DISTRICT_COLLECTION\",\"name\": \"DELETE_SDC_DISTRICT_COLLECTION\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope WRITE_SDC_COLLECTION
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Write Student Data Collection Collection Data\",\"id\": \"WRITE_SDC_COLLECTION\",\"name\": \"WRITE_SDC_COLLECTION\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope DELETE_SDC_COLLECTION
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Delete Student Data Collection Collection Data\",\"id\": \"DELETE_SDC_COLLECTION\",\"name\": \"DELETE_SDC_COLLECTION\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope WRITE_SDC_SCHOOL_COLLECTION
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Write Student Data Collection School Collection Data\",\"id\": \"WRITE_SDC_SCHOOL_COLLECTION\",\"name\": \"WRITE_SDC_SCHOOL_COLLECTION\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope DELETE_SDC_SCHOOL_COLLECTION
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Delete Student Data Collection School Collection Data\",\"id\": \"DELETE_SDC_SCHOOL_COLLECTION\",\"name\": \"DELETE_SDC_SCHOOL_COLLECTION\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope READ_COLLECTION_CODES
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Read Student Data Collection Collection Codes\",\"id\": \"READ_COLLECTION_CODES\",\"name\": \"READ_COLLECTION_CODES\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope READ_SDC_SCHOOL_COLLECTION_STUDENT
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Read Student Data Collection School Collection Students\",\"id\": \"READ_SDC_SCHOOL_COLLECTION_STUDENT\",\"name\": \"READ_SDC_SCHOOL_COLLECTION_STUDENT\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope READ_SCHOOL_FUNDING_GROUP_SNAPSHOT
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Read Independent School Funding Group Snapshot\",\"id\": \"READ_SCHOOL_FUNDING_GROUP_SNAPSHOT\",\"name\": \"READ_SCHOOL_FUNDING_GROUP_SNAPSHOT\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope WRITE_SDC_SCHOOL_COLLECTION_STUDENT
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Write Student Data Collection School Collection Students\",\"id\": \"WRITE_SDC_SCHOOL_COLLECTION_STUDENT\",\"name\": \"WRITE_SDC_SCHOOL_COLLECTION_STUDENT\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

  echo
  echo Writing scope DELETE_SDC_SCHOOL_COLLECTION_STUDENT
  curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TKN" \
    -d "{\"description\": \"Delete Student Data Collection School Collection Students\",\"id\": \"DELETE_SDC_SCHOOL_COLLECTION_STUDENT\",\"name\": \"DELETE_SDC_SCHOOL_COLLECTION_STUDENT\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

if [[ "$SDC_APIServiceClientSecret" != "" && ("$envValue" = "dev" || "$envValue" = "test") ]]; then
  echo
  echo Creating client student-data-collection-api-service with secret
  curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TKN" \
    -d "{\"clientId\" : \"student-data-collection-api-service\",\"secret\" : \"$SDC_APIServiceClientSecret\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {\"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",\"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { },\"fullScopeAllowed\" : true,\"nodeReRegistrationTimeout\" : -1,\"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientHost\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"READ_INSTITUTE_CODES\", \"READ_SCHOOL\", \"READ_DISTRICT\", \"READ_EDX_USERS\",\"web-origins\", \"role_list\", \"profile\", \"roles\", \"email\"],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"
else
  echo
    echo Creating client student-data-collection-api-service without secret
    curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TKN" \
      -d "{\"clientId\" : \"student-data-collection-api-service\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {\"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",\"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { },\"fullScopeAllowed\" : true,\"nodeReRegistrationTimeout\" : -1,\"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientHost\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"READ_INSTITUTE_CODES\", \"READ_SCHOOL\", \"READ_DISTRICT\", \"READ_EDX_USERS\", \"web-origins\", \"role_list\", \"profile\", \"roles\", \"email\"],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"
fi

echo
echo Retrieving client ID for student-data-collection-api-service
SDC_APIServiceClientID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq '.[] | select(.clientId=="student-data-collection-api-service")' | jq -r '.id')

echo
echo Retrieving client secret for student-data-collection-api-service
SDC_APIServiceClientSecret=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$SDC_APIServiceClientID/client-secret" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq -r '.value')
###########################################################
#Setup for config-map
###########################################################
SPLUNK_URL="gww.splunk.educ.gov.bc.ca"
FLB_CONFIG="[SERVICE]
   Flush        1
   Daemon       Off
   Log_Level    debug
   HTTP_Server   On
   HTTP_Listen   0.0.0.0
   Parsers_File parsers.conf
[INPUT]
   Name   tail
   Path   /mnt/log/*
   Exclude_Path *.gz,*.zip
   Parser docker
   Mem_Buf_Limit 20MB
[FILTER]
   Name record_modifier
   Match *
   Record hostname \${HOSTNAME}
[OUTPUT]
   Name   stdout
   Match  *
[OUTPUT]
   Name  splunk
   Match *
   Host  $SPLUNK_URL
   Port  443
   TLS         On
   TLS.Verify  Off
   Message_Key $APP_NAME
   Splunk_Token $SPLUNK_TOKEN
"
PARSER_CONFIG="
[PARSER]
    Name        docker
    Format      json
"

EMAIL_SUBJECT_COLLECTION_INDEPENDENT_SCHOOL_NOACTIVITY_NOTIFICATION="1701 File Not Yet Received for School"
EMAIL_TEMPLATE_COLLECTION_INDEPENDENT_SCHOOL_NOACTIVITY_NOTIFICATION="<!DOCTYPE html><html xmlns:th=\"http://www.thymeleaf.org\"><head><meta charset=\"ISO-8859-1\"><title>1701 File Not Yet Received for School</title></head><body>Dear Independent School Principal,<br/><br/>You are receiving this email because we have not yet received the 1701 submission from your school that was due on ${submissionDueDate}. Please submit your file as soon as possible through <a href=\"https://educationdataexchange.gov.bc.ca\">EDX</a>.<br/><br/>Instructions and guides for this data collection can be found at <a href=\"https://www2.gov.bc.ca/gov/content/education-training/k-12/administration/program-management/data-collections/${$}{dataCollectionMonth}\">https://www2.gov.bc.ca/gov/content/education-training/k-12/administration/program-management/data-collections/${dataCollectionMonth}</a><br><br><br><b>The Data Management Unit Team</b><br>Ministry of Education and Child Care<br>educationdataexchange@gov.bc.ca</body></html>"

EMAIL_SUBJECT_COLLECTION_INDEPENDENT_SCHOOL_NOTSUBMITTED_NOTIFICATION="1701 Submission Not Completed for School"
EMAIL_TEMPLATE_COLLECTION_INDEPENDENT_SCHOOL_NOTSUBMITTED_NOTIFICATION="<!DOCTYPE html><html xmlns:th=\"http://www.thymeleaf.org\"><head><meta charset=\"ISO-8859-1\"><title>1701 File Not Yet Received for School</title></head><body>Dear Independent School Principal,<br/><br/>You are receiving this email because your school has yet to complete the 1701 submission that was due on ${submissionDueDate}. We can see that you have loaded a file to <a href=\"https://educationdataexchange.gov.bc.ca\">EDX</a> but have not completed your submission. Please log into EDX and complete your 1701 submission as soon possible.<br/><br/>Instructions and guides for this data collection can be found at <a href=\"https://www2.gov.bc.ca/gov/content/education-training/k-12/administration/program-management/data-collections/${$}{dataCollectionMonth}\">https://www2.gov.bc.ca/gov/content/education-training/k-12/administration/program-management/data-collections/${dataCollectionMonth}</a><br><br><br><b>The Data Management Unit Team</b><br>Ministry of Education and Child Care<br>educationdataexchange@gov.bc.ca</body></html>"

EMAIL_SUBJECT_COLLECTION_PROVINCIAL_DUPLICATES_NOTIFICATION="1701 Provincial Duplicates Found for School"
EMAIL_TEMPLATE_COLLECTION_PROVINCIAL_DUPLICATES_NOTIFICATION="<!DOCTYPE html><html xmlns:th=\"http://www.thymeleaf.org\"><head><meta charset=\"ISO-8859-1\"><title>Edx School User Activation</title></head><body>One or more 1701 Provincial duplicates has been identified for the following school: <span th:text=\"\${$}{schoolName}\"></span>.<br><br>Please log into the EDX platform and open the collection to view and resolve your duplicates: <a href=\"https://educationdataexchange.gov.bc.ca\">https://educationdataexchange.gov.bc.ca</a><br><br></body></html>"

THREADS_MIN_SUBSCRIBER=8
THREADS_MAX_SUBSCRIBER=12
SAGAS_MAX_PENDING=150
SAGAS_MAX_PARALLEL=150
SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON="0 0/2 * * * *"
SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_LEAST_FOR="PT4M"
SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_MOST_FOR="PT4M"
SCHEDULED_JOBS_PROCESS_LOADED_SDC_STUDENTS_CRON="0/2 * * * * *"
SCHEDULED_JOBS_PROCESS_LOADED_SDC_STUDENTS_CRON_LOCK_AT_LEAST_FOR="1700ms"
SCHEDULED_JOBS_PROCESS_LOADED_SDC_STUDENTS_CRON_LOCK_AT_MOST_FOR="1900ms"
SCHEDULED_JOBS_PROCESS_SCHOOL_COLLECTION_FOR_SUBMISSION_CRON="0/2 * * * * *"
SCHEDULED_JOBS_PROCESS_SCHOOL_COLLECTION_FOR_SUBMISSION_CRON_LOCK_AT_LEAST_FOR="1700ms"
SCHEDULED_JOBS_PROCESS_SCHOOL_COLLECTION_FOR_SUBMISSION_CRON_LOCK_AT_MOST_FOR="1900ms"
SCHEDULED_JOBS_TARDY_INDY_SCHOOLS_CRON="@midnight"
SCHEDULED_JOBS_TARDY_INDY_SCHOOLS_CRON_LOCK_AT_LEAST_FOR="PT4M"
SCHEDULED_JOBS_TARDY_INDY_SCHOOLS_CRON_LOCK_AT_MOST_FOR="PT4M"

SCHOOL_NOTIFICATION_EMAIL_FROM="educationdataexchange@gov.bc.ca"

MAXIMUM_DB_POOL_SIZE=25
MINIMUM_IDLE_DB_POOL_SIZE=15
NUMBER_OF_STUDENTS_TO_PROCESS_SAGA=500

if [ "$envValue" = "dev" ]
then
  SCHOOL_NOTIFICATION_EMAIL_FROM="dev.educationdataexchange@gov.bc.ca"
elif [ "$envValue" = "test" ]
then
  SCHOOL_NOTIFICATION_EMAIL_FROM="test.educationdataexchange@gov.bc.ca"
fi

echo
echo Creating config map "$APP_NAME"-config-map
oc create -n "$OPENSHIFT_NAMESPACE"-"$envValue" configmap "$APP_NAME"-config-map --from-literal=TZ=$TZVALUE --from-literal=EDX_API_URL="http://edx-api-master.$OPENSHIFT_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/edx" --from-literal=SCHOOL_NOTIFICATION_EMAIL_FROM=$SCHOOL_NOTIFICATION_EMAIL_FROM --from-literal=SCHEDULED_JOBS_TARDY_INDY_SCHOOLS_CRON_LOCK_AT_MOST_FOR="$SCHEDULED_JOBS_TARDY_INDY_SCHOOLS_CRON_LOCK_AT_MOST_FOR" --from-literal=SCHEDULED_JOBS_TARDY_INDY_SCHOOLS_CRON_LOCK_AT_LEAST_FOR="$SCHEDULED_JOBS_TARDY_INDY_SCHOOLS_CRON_LOCK_AT_LEAST_FOR" --from-literal=SCHEDULED_JOBS_TARDY_INDY_SCHOOLS_CRON="$SCHEDULED_JOBS_TARDY_INDY_SCHOOLS_CRON" --from-literal=EMAIL_TEMPLATE_COLLECTION_INDEPENDENT_SCHOOL_NOTSUBMITTED_NOTIFICATION="$EMAIL_TEMPLATE_COLLECTION_INDEPENDENT_SCHOOL_NOTSUBMITTED_NOTIFICATION" --from-literal=EMAIL_TEMPLATE_COLLECTION_PROVINCIAL_DUPLICATES_NOTIFICATION="$EMAIL_TEMPLATE_COLLECTION_PROVINCIAL_DUPLICATES_NOTIFICATION" --from-literal=EMAIL_SUBJECT_COLLECTION_PROVINCIAL_DUPLICATES_NOTIFICATION="$EMAIL_SUBJECT_COLLECTION_PROVINCIAL_DUPLICATES_NOTIFICATION" --from-literal=EMAIL_SUBJECT_COLLECTION_INDEPENDENT_SCHOOL_NOTSUBMITTED_NOTIFICATION="$EMAIL_SUBJECT_COLLECTION_INDEPENDENT_SCHOOL_NOTSUBMITTED_NOTIFICATION" --from-literal=EMAIL_TEMPLATE_COLLECTION_INDEPENDENT_SCHOOL_NOACTIVITY_NOTIFICATION="$EMAIL_TEMPLATE_COLLECTION_INDEPENDENT_SCHOOL_NOACTIVITY_NOTIFICATION" --from-literal=EMAIL_SUBJECT_COLLECTION_INDEPENDENT_SCHOOL_NOACTIVITY_NOTIFICATION="$EMAIL_SUBJECT_COLLECTION_INDEPENDENT_SCHOOL_NOACTIVITY_NOTIFICATION" --from-literal=CHES_CLIENT_ID="$CHES_CLIENT_ID" --from-literal=CHES_CLIENT_SECRET="$CHES_CLIENT_SECRET" --from-literal=CHES_TOKEN_URL="$CHES_TOKEN_URL" --from-literal=CHES_ENDPOINT_URL="$CHES_ENDPOINT_URL" --from-literal=JDBC_URL="$DB_JDBC_CONNECT_STRING" --from-literal=PURGE_RECORDS_SAGA_AFTER_DAYS=400 --from-literal=SCHEDULED_JOBS_PURGE_OLD_SAGA_RECORDS_CRON="@midnight" --from-literal=DB_USERNAME="$DB_USER" --from-literal=DB_PASSWORD="$DB_PWD" --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --from-literal=SPRING_JPA_SHOW_SQL="false" --from-literal=TOKEN_ISSUER_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID" --from-literal=NATS_MAX_RECONNECT=60 --from-literal=NATS_URL=$NATS_URL --from-literal=CLIENT_ID="student-data-collection-api-service" --from-literal=CLIENT_SECRET="$SDC_APIServiceClientSecret" --from-literal=THREADS_MIN_SUBSCRIBER="$THREADS_MIN_SUBSCRIBER" --from-literal=THREADS_MAX_SUBSCRIBER="$THREADS_MAX_SUBSCRIBER" --from-literal=SAGAS_MAX_PENDING="$SAGAS_MAX_PENDING" --from-literal=SAGAS_MAX_PARALLEL="$SAGAS_MAX_PARALLEL" --from-literal=TOKEN_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token" --from-literal=SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON="$SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON" --from-literal=SCHEDULED_JOBS_PROCESS_LOADED_SDC_STUDENTS_CRON="$SCHEDULED_JOBS_PROCESS_LOADED_SDC_STUDENTS_CRON" --from-literal=SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_LEAST_FOR="$SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_LEAST_FOR" --from-literal=SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_MOST_FOR="$SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_MOST_FOR" --from-literal=SCHEDULED_JOBS_PROCESS_LOADED_SDC_STUDENTS_CRON_LOCK_AT_LEAST_FOR="$SCHEDULED_JOBS_PROCESS_LOADED_SDC_STUDENTS_CRON_LOCK_AT_LEAST_FOR" --from-literal=SCHEDULED_JOBS_PROCESS_LOADED_SDC_STUDENTS_CRON_LOCK_AT_MOST_FOR="$SCHEDULED_JOBS_PROCESS_LOADED_SDC_STUDENTS_CRON_LOCK_AT_MOST_FOR" --from-literal=INSTITUTE_API_URL="http://institute-api-master.$COMMON_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/institute" --from-literal=MAXIMUM_DB_POOL_SIZE="$MAXIMUM_DB_POOL_SIZE" --from-literal=MINIMUM_IDLE_DB_POOL_SIZE="$MINIMUM_IDLE_DB_POOL_SIZE" --from-literal=NUMBER_OF_STUDENTS_TO_PROCESS_SAGA="$NUMBER_OF_STUDENTS_TO_PROCESS_SAGA" --from-literal=SCHEDULED_JOBS_PROCESS_SCHOOL_COLLECTION_FOR_SUBMISSION_CRON_LOCK_AT_MOST_FOR="$SCHEDULED_JOBS_PROCESS_SCHOOL_COLLECTION_FOR_SUBMISSION_CRON_LOCK_AT_MOST_FOR" --from-literal=SCHEDULED_JOBS_PROCESS_SCHOOL_COLLECTION_FOR_SUBMISSION_CRON_LOCK_AT_LEAST_FOR="$SCHEDULED_JOBS_PROCESS_SCHOOL_COLLECTION_FOR_SUBMISSION_CRON_LOCK_AT_LEAST_FOR" --from-literal=SCHEDULED_JOBS_PROCESS_SCHOOL_COLLECTION_FOR_SUBMISSION_CRON="$SCHEDULED_JOBS_PROCESS_SCHOOL_COLLECTION_FOR_SUBMISSION_CRON"  --dry-run -o yaml | oc apply -f -

echo
echo Setting environment variables for $APP_NAME-$SOAM_KC_REALM_ID application
oc -n "$OPENSHIFT_NAMESPACE"-"$envValue" set env --from=configmap/$APP_NAME-config-map dc/$APP_NAME-$SOAM_KC_REALM_ID

echo Creating config map "$APP_NAME"-flb-sc-config-map
oc create -n "$OPENSHIFT_NAMESPACE"-"$envValue" configmap "$APP_NAME"-flb-sc-config-map --from-literal=fluent-bit.conf="$FLB_CONFIG" --from-literal=parsers.conf="$PARSER_CONFIG" --dry-run -o yaml | oc apply -f -
