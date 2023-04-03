envValue=$1
APP_NAME=$2
OPENSHIFT_NAMESPACE=$3
COMMON_NAMESPACE=$4
APP_NAME_UPPER=${APP_NAME^^}
DB_JDBC_CONNECT_STRING=$5
DB_PWD=$6
DB_USER=$7
SPLUNK_TOKEN=$8
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
SDC_CLIENT_ID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq '.[] | select(.clientId=="student-data-collection-api-service")' | jq -r '.id')

echo
echo Removing STUDENT DATA COLLECTION API client if exists
curl -sX DELETE "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$SDC_CLIENT_ID" \
  -H "Authorization: Bearer $TKN"

echo
echo Writing scope READ_SDC_COLLECTION
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Read Student Data Collection Collection Data\",\"id\": \"READ_SDC_COLLECTION\",\"name\": \"READ_SDC_COLLECTION\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

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
echo Creating client student-data-collection-api-service
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"clientId\" : \"student-data-collection-api-service\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {\"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",\"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { },\"fullScopeAllowed\" : true,\"nodeReRegistrationTimeout\" : -1,\"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientHost\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"READ_INSTITUTE_CODES\", \"READ_SCHOOL\", \"web-origins\", \"role_list\", \"profile\", \"roles\", \"email\"],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"

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

THREADS_MIN_SUBSCRIBER=4
THREADS_MAX_SUBSCRIBER=8
SAGAS_MAX_PENDING=100
SAGAS_MAX_PARALLEL=100
CRON_SCHEDULED_START_COLLECTION="@midnight"
CRON_SCHEDULED_START_COLLECTION_LOCK_AT_MOST_FOR="PT4M"
CRON_SCHEDULED_START_COLLECTION_LOCK_AT_LEAST_FOR="PT4M"
SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON="0 0/2 * * * *"
SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_LEAST_FOR="PT4M"
SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_MOST_FOR="PT4M"
SCHEDULED_JOBS_PROCESS_LOADED_SDC_STUDENTS_CRON="0/2 * * * * *"
SCHEDULED_JOBS_PROCESS_LOADED_SDC_STUDENTS_CRON_LOCK_AT_LEAST_FOR="1700ms"
SCHEDULED_JOBS_PROCESS_LOADED_SDC_STUDENTS_CRON_LOCK_AT_MOST_FOR="1900ms"
MAXIMUM_DB_POOL_SIZE=10
MINIMUM_IDLE_DB_POOL_SIZE=10
#SOFT_DELETED_RETENTION_DAYS=365
#SCHEDULED_JOBS_EXTRACT_UNPROCESSED_PENWEB_PEN_WEB_BLOBS_CRON="-"
#SCHEDULED_JOBS_EXTRACT_UNPROCESSED_PEN_WEB_BLOBS_CRON="0 0/10 * * * *"
#SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON="0 0/1 * * * *"
#SCHEDULED_JOBS_EXTRACT_UNPROCESSED_STUDENTS_CRON="0/10 * * * * *"
#SCHEDULED_JOBS_PURGE_SOFT_DELETED_RECORDS_CRON="@midnight"
#SCHEDULED_JOBS_PURGE_OLD_SAGA_RECORDS_CRON="@midnight"
#SCHEDULED_JOBS_MARK_PROCESSED_BATCHES_ACTIVE_CRON="0 0/1 * * * *"
#SCHEDULED_JOBS_PROCESS_LOADED_BATCHES_FOR_REPEATS_CRON="0 0/2 * * * *"

echo
echo Creating config map "$APP_NAME"-config-map
oc create -n "$OPENSHIFT_NAMESPACE"-"$envValue" configmap "$APP_NAME"-config-map --from-literal=TZ=$TZVALUE --from-literal=JDBC_URL="$DB_JDBC_CONNECT_STRING" --from-literal=DB_USERNAME="$DB_USER" --from-literal=DB_PASSWORD="$DB_PWD" --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --from-literal=SPRING_JPA_SHOW_SQL="false" --from-literal=TOKEN_ISSUER_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID" --from-literal=NATS_MAX_RECONNECT=60 --from-literal=NATS_URL=$NATS_URL --from-literal=CLIENT_ID="student-data-collection-api-service" --from-literal=CLIENT_SECRET="$SDC_APIServiceClientSecret" --from-literal=THREADS_MIN_SUBSCRIBER="$THREADS_MIN_SUBSCRIBER" --from-literal=THREADS_MAX_SUBSCRIBER="$THREADS_MAX_SUBSCRIBER" --from-literal=SAGAS_MAX_PENDING="$SAGAS_MAX_PENDING" --from-literal=SAGAS_MAX_PARALLEL="$SAGAS_MAX_PARALLEL" --from-literal=TOKEN_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token" --from-literal=CRON_SCHEDULED_START_COLLECTION="$CRON_SCHEDULED_START_COLLECTION" --from-literal=CRON_SCHEDULED_START_COLLECTION_LOCK_AT_LEAST_FOR="$CRON_SCHEDULED_START_COLLECTION_LOCK_AT_LEAST_FOR" --from-literal=CRON_SCHEDULED_START_COLLECTION_LOCK_AT_MOST_FOR="$CRON_SCHEDULED_START_COLLECTION_LOCK_AT_MOST_FOR" --from-literal=SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON="$SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON" --from-literal=SCHEDULED_JOBS_PROCESS_LOADED_SDC_STUDENTS_CRON="$SCHEDULED_JOBS_PROCESS_LOADED_SDC_STUDENTS_CRON" --from-literal=SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_LEAST_FOR="$SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_LEAST_FOR" --from-literal=SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_MOST_FOR="$SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_MOST_FOR" --from-literal=SCHEDULED_JOBS_PROCESS_LOADED_SDC_STUDENTS_CRON_LOCK_AT_LEAST_FOR="$SCHEDULED_JOBS_PROCESS_LOADED_SDC_STUDENTS_CRON_LOCK_AT_LEAST_FOR" --from-literal=SCHEDULED_JOBS_PROCESS_LOADED_SDC_STUDENTS_CRON_LOCK_AT_MOST_FOR="$SCHEDULED_JOBS_PROCESS_LOADED_SDC_STUDENTS_CRON_LOCK_AT_MOST_FOR" --from-literal=INSTITUTE_API_URL="http://institute-api-master.$COMMON_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/institute" --from-literal=MAXIMUM_DB_POOL_SIZE="$MAXIMUM_DB_POOL_SIZE" --from-literal=MINIMUM_IDLE_DB_POOL_SIZE="$MINIMUM_IDLE_DB_POOL_SIZE" --dry-run -o yaml | oc apply -f -

echo
echo Setting environment variables for $APP_NAME-$SOAM_KC_REALM_ID application
oc -n "$OPENSHIFT_NAMESPACE"-"$envValue" set env --from=configmap/$APP_NAME-config-map dc/$APP_NAME-$SOAM_KC_REALM_ID

echo Creating config map "$APP_NAME"-flb-sc-config-map
oc create -n "$OPENSHIFT_NAMESPACE"-"$envValue" configmap "$APP_NAME"-flb-sc-config-map --from-literal=fluent-bit.conf="$FLB_CONFIG" --from-literal=parsers.conf="$PARSER_CONFIG" --dry-run -o yaml | oc apply -f -
