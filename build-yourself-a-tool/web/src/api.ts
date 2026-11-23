import { fetchTransport } from '@polyvariant/smithy-ts-runtime'
import { PreviewServiceClient, TalkServiceClient } from './generated'

const transport = fetchTransport()

export const talkApi = new TalkServiceClient(transport)
export const previewApi = new PreviewServiceClient(transport)
