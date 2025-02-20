import { useMutation } from 'react-fetching-library';

import { Operations } from '../generated/OpenapiInternal';

export type RenderEmailRequest = {
    subject: string;
    body: string;
    payload: string;
}

const actionCreator = (params: RenderEmailRequest) => Operations.InternalServiceRenderEmailTemplate.actionCreator({
    body: {
        subject_template: params.subject,
        body_template: params.body,
        payload: params.payload
    }
});

export const useRenderEmailRequest = () => {
    return useMutation(actionCreator);
};
