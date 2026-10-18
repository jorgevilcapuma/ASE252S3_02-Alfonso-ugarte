import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ComunicadosComponent } from './comunicados.component';

describe('ComunicadosComponent', () => {
  let component: ComunicadosComponent;
  let fixture: ComponentFixture<ComunicadosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ComunicadosComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(ComunicadosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});