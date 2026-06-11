import os
import psutil
process = psutil.Process(os.getpid())
print("[INFO] MEM CONSUMED BEFORE PACKAGES IMPORT: "+str(process.memory_info().rss/ 1024 ** 2)+" MB")
import uuid

flag = False

from ionml_current_dir import *
from flask import Flask
from flask import request
app = Flask(__name__)

import time
import ionml_RINX as RINX
import os, ssl
import shutil
import ionml_load_spacy_utility as load_spacy_utility
from ionml_CRF_configuration import CRF_MODELS_DIR
from ionml_process_labeldata_utility import read_entity_types
from ionml_CRF_model_load_utility import *
from ionml_LSTM_model_load_utility import *
from ionml_LSTM_Configuration import *
from ionml_Relation_Association_model_loading_utility import *

#os.environ['NLTK_DATA'] = 'C:\\Users\\1597932\\Downloads\\nltk_data'
#os.environ['PDFBOX'] = 'C:\\RinxWs\\resources\\pdfbox-app-2.0.23.jar'
os.environ['PDFBOX'] = os.path.join(ENVIRONMENT_DIR_NAME, 'pdfbox-app-2.0.23.jar')
if (not os.environ.get('PYTHONHTTPSVERIFY', '') and
    getattr(ssl, '_create_unverified_context', None)):
    ssl._create_default_https_context = ssl._create_unverified_context

spacy_model = load_spacy_utility.load_spacy_model()
print("Spacy Model Loaded")

etypes = read_entity_types()
crf_models = CRF_load_model(CRF_MODELS_DIR, etypes)
print("CRF Model Loaded")

model_dict = {}
model_LSTM, word_to_index, pos_to_index, ner_to_index, graph_LSTM, sess_LSTM = LSTM_Entity_Extraction_load_model_and_parameters(LSTM_model_file, parameters_file)
model_dict["model"] = model_LSTM
model_dict["word_to_index"] = word_to_index
model_dict["pos_to_index"] = pos_to_index
model_dict["ner_to_index"] = ner_to_index
model_dict["graph"] = graph_LSTM
model_dict["sess"] = sess_LSTM
print("LSTM Model Loaded")

#word_vectors_file = "C:\\RinxWs\\resources\\RINX_DATA\\data\\relation_extraction\\glove.6B.100d.txt"
word_vectors_file = WORD_VECTOR_FILE
encoder_model_file = ENCODER_MODEL_FILE
model_dir = MODEL_DIR
only_encoder, model_relation, relation_model_to_lst_section_labels, word_embeddings, sess_relation , graph_relation = load_relation_association_model(word_vectors_file, encoder_model_file, model_dir)
relation_models = {}
relation_models["only_encoder"] = only_encoder
relation_models["models"] = model_relation
relation_models["relation_model_to_lst_section_labels"] = relation_model_to_lst_section_labels
relation_models["word_embeddings"] = word_embeddings
relation_models["sess"] = sess_relation
relation_models["graph"] = graph_relation
print("Relation Association Model Loaded")

print("[INFO] MEM CONSUMED AFTER PACKAGES IMPORT: "+str(process.memory_info().rss/ 1024 ** 2)+" MB")

flag = True
print("Gunicorn ready to take request: flag value ", flag)

@app.route('/rinxTest', methods=['POST'])
def rinxTest():
    if flag == True:
        return("--Ready to take requests")
    else:
        return("--Not ready"),201

@app.route('/rinx', methods=['POST'])
def rinx():
    unique_id = str(uuid.uuid1())
    print(request.json)
    start=time.time()
    content=request.json
    folderPath=content['inputFilePath']
    outputFolderPath=content['outputFolderPath']
    print("Folder path recieved ", folderPath)
    print("outputFolderPath ", outputFolderPath)
    debugMode = True
    #result = RINX.main(folderPath, debugMode, time, Path, os, shutil, DocToTextConversion, warnings, docx2html, text_conv_config, subprocess)
    result = RINX.main(folderPath, debugMode, spacy_model, crf_models, etypes, model_dict, relation_models, outputFolderPath, unique_id)

    '''
    foldernames = ['cvparse_text', 'cvparse_intermediate', 'cvparse_output', 'cvparse_temp',
                    'newRinxFolder', 'skill_proficiency_output','PostProcessedTextOutput']
    '''
    foldernames = [f'cvparse_text_{unique_id}', f'cvparse_intermediate_{unique_id}', f'cvparse_output_{unique_id}', f'cvparse_temp_{unique_id}',
                    f'newRinxFolder_{unique_id}', 'skill_proficiency_output','PostProcessedTextOutput']
                    
    from pathlib import Path
    try:
        ipp_path = str(Path(folderPath).parent)
        print(ipp_path)
        for inner_folder in foldernames:
            folder_to_delete = os.path.join(ipp_path,inner_folder)
            print('folder_to_delete:',folder_to_delete)
            if os.path.isdir(folder_to_delete):
                shutil.rmtree(folder_to_delete)
        
        if os.path.exists(os.path.join(ipp_path,'run.ini')):
            os.remove(os.path.join(ipp_path,'run.ini'))
    except Exception as e:
        print('Exception in deletion:', e)
        
    endTime=time.time()-start
    print(endTime)
    print(result)
    return result

if __name__=='__main__':
    app.run(host='0.0.0.0', threaded=False)