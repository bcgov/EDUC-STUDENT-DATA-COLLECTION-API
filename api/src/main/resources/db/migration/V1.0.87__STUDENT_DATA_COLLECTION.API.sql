alter table SDC_SAGA_EVENT_STATES DROP CONSTRAINT sdc_saga_event_states_saga_id_fk;
ALTER TABLE SDC_SAGA_EVENT_STATES ADD CONSTRAINT sdc_saga_event_states_saga_id_fk FOREIGN KEY (saga_id) REFERENCES sdc_saga(saga_id) on delete cascade;
